package de.unistuttgart.stayinsync.core.configuration.service.aas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasSubmodelLite;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasElementLite;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.inject.Inject;

import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class AasStructureSnapshotService {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    AasTraversalClient traversalClient;

    @Inject
    HttpHeaderBuilder headerBuilder;

    @Transactional
    public void buildInitialSnapshot(Long sourceSystemId) {
        refreshSnapshot(sourceSystemId);
    }

    @Transactional
    public void refreshSnapshot(Long sourceSystemId) {
        var ssOpt = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId);
        if (ssOpt.isEmpty()) {
            Log.warnf("No SourceSystem found with id=%d", sourceSystemId);
            return;
        }
        SourceSystem ss = ssOpt.get();

        try {
            AasElementLite.delete("submodelLite.sourceSystem.id", sourceSystemId);
            AasSubmodelLite.delete("sourceSystem.id", sourceSystemId);

            var headers = headerBuilder.buildMergedHeaders(ss, HttpHeaderBuilder.Mode.READ);
            Uni<HttpResponse<Buffer>> uni = traversalClient.listSubmodels(ss.apiUrl, ss.aasId, headers);
            HttpResponse<Buffer> resp = uni.await().indefinitely();
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                Log.warnf("Skipping snapshot refresh due to upstream status %d %s", resp.statusCode(), resp.statusMessage());
                return;
            }

            String json = resp.bodyAsString();
            JsonNode root = objectMapper.readTree(json);
            if (root == null) return;

            if (root.isArray()) {
                for (JsonNode n : root) {
                    persistSubmodelLite(ss, n);
                }
            } else if (root.has("result") && root.get("result").isArray()) {
                for (JsonNode n : root.get("result")) {
                    persistSubmodelLite(ss, n);
                }
            } else {
                persistSubmodelLite(ss, root);
            }

            var submodels = AasSubmodelLite.list("sourceSystem.id", sourceSystemId);
            for (Object o : submodels) {
                AasSubmodelLite sm = (AasSubmodelLite) o;
                String smIdB64 = toBase64Url(sm.submodelId);
                if (smIdB64 == null) continue;
                try {
                    Uni<HttpResponse<Buffer>> ue = traversalClient.listElements(ss.apiUrl, smIdB64, "shallow", null, headers);
                    HttpResponse<Buffer> er = ue.await().indefinitely();
                    if (er.statusCode() >= 200 && er.statusCode() < 300) {
                        String ejson = er.bodyAsString();
                        JsonNode en = objectMapper.readTree(ejson);
                        if (en == null) continue;
                        Set<String> seen = new HashSet<>();
                        if (en.isArray()) {
                            for (JsonNode node : en) {
                                persistElementLite(sm, null, node, seen);
                            }
                        } else if (en.has("result") && en.get("result").isArray()) {
                            for (JsonNode node : en.get("result")) {
                                persistElementLite(sm, null, node, seen);
                            }
                        }
                    }
                } catch (Exception ex) {
                    Log.warnf(ex, "Failed to persist shallow elements for submodel %s", sm.submodelId);
                }
            }
        } catch (Exception e) {
            Log.errorf(e, "Failed to refresh AAS snapshot for sourceSystemId=%d", sourceSystemId);
        }
    }

    public static class InvalidAasxException extends RuntimeException {
        public InvalidAasxException(String message) { super(message); }
    }

    public static class DuplicateIdException extends RuntimeException {
        public DuplicateIdException(String message) { super(message); }
    }

    @Transactional
    public void ingestAasx(Long sourceSystemId, String filename, byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new InvalidAasxException("Empty file");
        }
        var ssOpt = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId);
        if (ssOpt.isEmpty()) {
            throw new InvalidAasxException("Unknown source system: " + sourceSystemId);
        }
        SourceSystem ss = ssOpt.get();

        int importedSubmodels = 0;
        boolean foundAnySubmodelPayload = false;
        // First, load all ZIP JSON entries into memory for easier cross-referencing
        final java.util.List<ZipJson> jsonEntries = new java.util.ArrayList<>();
        boolean hasEntries = false;
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fileBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null) continue;
                hasEntries = true;
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                if (lower.endsWith(".xml") && (lower.contains("submodel") || lower.contains("sub-model"))) {
                    foundAnySubmodelPayload = true; // xml present
                }
                if (!lower.endsWith(".json")) continue;
                try {
                    String json = new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    jsonEntries.add(new ZipJson(name, lower, json));
                } catch (Exception ignore) { /* skip unreadable */ }
            }
        } catch (java.io.IOException e) {
            throw new InvalidAasxException("Failed to read AASX: " + e.getMessage());
        }
        if (!hasEntries) {
            throw new InvalidAasxException("Not a valid AASX: archive has no entries");
        }

        // Heuristics: consider JSON as Submodel if it declares modelType Submodel or contains submodelElements
        for (ZipJson zj : jsonEntries) {
            try {
                JsonNode root = objectMapper.readTree(zj.json);
                if (root == null) continue;
                boolean looksLikeSubmodel =
                        (root.has("modelType") && "Submodel".equalsIgnoreCase(textOrNull(root, "modelType")))
                        || (root.has("submodelElements") && root.get("submodelElements").isArray());
                if (!looksLikeSubmodel) continue;
                String id = textOrNull(root, "id");
                if (id == null && root.has("identification")) {
                    id = textOrNull(root.get("identification"), "id");
                }
                if (id == null || id.isBlank()) continue;
                foundAnySubmodelPayload = true;

                var existingSm = AasSubmodelLite.<AasSubmodelLite>find("sourceSystem.id = ?1 and submodelId = ?2", ss.id, id).firstResult();
                if (existingSm != null) {
                    throw new DuplicateIdException("Submodel already exists: " + id);
                }

                String idShort = textOrNull(root, "idShort");
                String semanticId = textOrNull(root, "semanticId");
                String kind = textOrNull(root, "kind");

                AasSubmodelLite sm = new AasSubmodelLite();
                sm.sourceSystem = ss;
                sm.submodelId = id;
                sm.submodelIdShort = idShort;
                sm.semanticId = semanticId;
                sm.kind = kind;
                sm.persist();
                importedSubmodels++;

                JsonNode elementsNode = root.get("submodelElements");
                if (elementsNode != null && elementsNode.isArray()) {
                    java.util.Set<String> seen = new java.util.HashSet<>();
                    for (JsonNode el : elementsNode) {
                        ingestElement(sm, null, el, seen);
                    }
                }
            } catch (Exception ex) {
                // ignore individual JSON failures
            }
        }

        // If no JSON was imported, try to extract minimal info from XML submodel entries (top-level elements)
        if (importedSubmodels == 0) {
            try (java.util.zip.ZipInputStream zis2 = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fileBytes))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis2.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name == null) continue;
                    String lower = name.toLowerCase(java.util.Locale.ROOT);
                    if (!(lower.endsWith(".xml") && (lower.contains("submodel") || lower.contains("sub-model")))) continue;
                    try {
                        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                        dbf.setNamespaceAware(true);
                        javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
                        org.w3c.dom.Document doc = db.parse(zis2);
                        org.w3c.dom.Element root = doc.getDocumentElement();
                        if (root == null) continue;
                        String rootLocal = root.getLocalName() != null ? root.getLocalName() : root.getNodeName();
                        if (rootLocal == null || !rootLocal.toLowerCase(java.util.Locale.ROOT).contains("submodel")) {
                            // try to find a nested submodel element
                            var subNodes = root.getElementsByTagNameNS("*", "submodel");
                            if (subNodes.getLength() > 0) {
                                root = (org.w3c.dom.Element) subNodes.item(0);
                            }
                        }
                        // IDTA templates often use identifier/@id as URN/IRI
                        String id = firstTextByLocalName(root, "id");
                        if (id == null) {
                            var ident = firstChildByLocalName(root, "identification");
                            if (ident != null) id = firstTextByLocalName(ident, "id");
                            if (id == null) {
                                // try generic identifier element
                                var identifier = firstChildByLocalName(root, "identifier");
                                if (identifier != null) {
                                    String i1 = identifier.getAttribute("id");
                                    if (i1 != null && !i1.isBlank()) id = i1;
                                    if (id == null) id = identifier.getTextContent();
                                }
                            }
                        }
                        if (id == null || id.isBlank()) {
                            // fallback to file name
                            id = name;
                        }
                        foundAnySubmodelPayload = true;
                        var existingSm = AasSubmodelLite.<AasSubmodelLite>find("sourceSystem.id = ?1 and submodelId = ?2", ss.id, id).firstResult();
                        if (existingSm != null) {
                            // skip duplicates
                            continue;
                        }
                        String idShort = firstTextByLocalName(root, "idShort");
                        String kind = firstTextByLocalName(root, "kind");
                        String semanticId = null;
                        var sem = firstChildByLocalName(root, "semanticId");
                        if (sem != null) semanticId = firstTextByLocalName(sem, "value");

                        AasSubmodelLite sm = new AasSubmodelLite();
                        sm.sourceSystem = ss;
                        sm.submodelId = id;
                        sm.submodelIdShort = idShort;
                        sm.semanticId = semanticId;
                        sm.kind = kind;
                        sm.persist();
                        importedSubmodels++;

                        // Top-level elements under submodelElements
                        // IDTA packages sometimes use pluralization or different casing
                        var smEls = firstChildByLocalName(root, "submodelElements");
                        if (smEls == null) smEls = firstChildByLocalName(root, "subModelElements");
                        if (smEls == null) smEls = firstChildByLocalName(root, "elements");
                        if (smEls != null) {
                            java.util.Set<String> seen = new java.util.HashSet<>();
                            var children = childElements(smEls);
                            for (org.w3c.dom.Element el : children) {
                                String elIdShort = firstTextByLocalName(el, "idShort");
                                if (elIdShort == null) continue;
                                String modelType = el.getLocalName() != null ? el.getLocalName() : el.getNodeName();
                                // Normalize common names
                                if (modelType != null) {
                                    String ml = modelType.toLowerCase(java.util.Locale.ROOT);
                                    if (ml.contains("property")) modelType = "Property";
                                    else if (ml.contains("submodelelementcollection")) modelType = "SubmodelElementCollection";
                                    else if (ml.contains("submodelelementlist")) modelType = "SubmodelElementList";
                                    else if (ml.contains("entity")) modelType = "Entity";
                                }
                                // Persist as shallow element
                                AasElementLite e = new AasElementLite();
                                e.submodelLite = sm;
                                e.idShort = elIdShort;
                                e.modelType = modelType;
                                e.idShortPath = elIdShort;
                                e.parentPath = null;
                                e.hasChildren = "SubmodelElementCollection".equalsIgnoreCase(modelType)
                                        || "SubmodelElementList".equalsIgnoreCase(modelType)
                                        || "Entity".equalsIgnoreCase(modelType);
                                e.persist();
                            }
                        }
                    } catch (Exception ignore) {
                        // ignore this XML entry
                    }
                }
            } catch (java.io.IOException e) {
                // ignore
            }
        }
        if (importedSubmodels == 0) {
            if (foundAnySubmodelPayload) {
                // Submodel content present but not JSON → accept and refresh from LIVE
                Log.infof("AASX contained submodel payloads but none imported (non-JSON). Triggering LIVE snapshot refresh for sourceSystemId=%d", sourceSystemId);
                refreshSnapshot(sourceSystemId);
                return;
            } else {
                // No recognizable submodel payload at all → still accept and trigger LIVE refresh as best-effort
                Log.infof("AASX contained no recognizable submodel payloads. Triggering LIVE snapshot refresh for sourceSystemId=%d", sourceSystemId);
                refreshSnapshot(sourceSystemId);
                return;
            }
        }
    }

    private void ingestElement(AasSubmodelLite submodel, String parentPath, JsonNode n, java.util.Set<String> seen) {
        String idShort = textOrNull(n, "idShort");
        if (idShort == null) return;
        String modelType = textOrNull(n, "modelType");
        String idShortPath = parentPath == null || parentPath.isBlank() ? idShort : parentPath + "/" + idShort;

        String key = submodel.id + "::" + idShortPath;
        if (seen != null) {
            if (seen.contains(key)) return;
            seen.add(key);
        }

        // Duplicate check for element path
        long count = AasElementLite.count("submodelLite.id = ?1 and idShortPath = ?2", submodel.id, idShortPath);
        if (count > 0) {
            throw new DuplicateIdException("Element already exists: " + idShortPath);
        }

        AasElementLite e = new AasElementLite();
        e.submodelLite = submodel;
        e.idShort = idShort;
        e.modelType = modelType;
        e.valueType = textOrNull(n, "valueType");
        e.idShortPath = idShortPath;
        e.parentPath = parentPath;
        e.semanticId = textOrNull(n, "semanticId");
        String tvle = textOrNull(n, "typeValueListElement");
        if (tvle == null) tvle = textOrNull(n, "valueTypeListElement");
        e.typeValueListElement = tvle;
        e.orderRelevant = n.has("orderRelevant") ? n.get("orderRelevant").asBoolean(false) : null;
        e.hasChildren = "SubmodelElementCollection".equalsIgnoreCase(modelType)
                || "SubmodelElementList".equalsIgnoreCase(modelType)
                || "Entity".equalsIgnoreCase(modelType);

        if ("Operation".equalsIgnoreCase(modelType)) {
            JsonNode inVars = n.get("inputVariables");
            JsonNode outVars = n.get("outputVariables");
            e.inputSignature = (inVars != null && !inVars.isNull()) ? inVars.toString() : null;
            e.outputSignature = (outVars != null && !outVars.isNull()) ? outVars.toString() : null;
        }
        if ("ReferenceElement".equalsIgnoreCase(modelType)) {
            JsonNode val = n.get("value");
            if (val != null) {
                e.isReference = true;
                e.referenceTargetType = textOrNull(val, "type");
                e.referenceKeys = val.has("keys") ? val.get("keys").toString() : null;
                if (val.has("keys") && val.get("keys").isArray() && val.get("keys").size() > 0) {
                    JsonNode k0 = val.get("keys").get(0);
                    if (k0 != null && "Submodel".equals(textOrNull(k0, "type"))) {
                        e.targetSubmodelId = textOrNull(k0, "value");
                    }
                }
            }
        }
        e.persist();

        // Recurse for collections/lists/entities that contain nested elements
        if (e.hasChildren) {
            // SubmodelElementCollection: value is array named "value"
            if ("SubmodelElementCollection".equalsIgnoreCase(modelType)) {
                JsonNode coll = n.get("value");
                if (coll != null && coll.isArray()) {
                    for (JsonNode child : coll) {
                        ingestElement(submodel, idShortPath, child, seen);
                    }
                }
            }
            // SubmodelElementList: value is array named "value"
            else if ("SubmodelElementList".equalsIgnoreCase(modelType)) {
                JsonNode list = n.get("value");
                if (list != null && list.isArray()) {
                    for (JsonNode child : list) {
                        ingestElement(submodel, idShortPath, child, seen);
                    }
                }
            }
            // Entity: has statements array
            else if ("Entity".equalsIgnoreCase(modelType)) {
                JsonNode stmts = n.get("statements");
                if (stmts != null && stmts.isArray()) {
                    for (JsonNode child : stmts) {
                        ingestElement(submodel, idShortPath, child, seen);
                    }
                }
            }
        }
    }

    private void persistSubmodelLite(SourceSystem ss, JsonNode n) {
        String id = textOrNull(n, "id");
        if (id == null && n.has("identification")) {
            id = textOrNull(n.get("identification"), "id");
        }
        if (id == null && n.has("keys") && n.get("keys").isArray() && n.get("keys").size() > 0) {
            JsonNode k0 = n.get("keys").get(0);
            if (k0 != null && "Submodel".equals(textOrNull(k0, "type"))) {
                id = textOrNull(k0, "value");
            }
        }
        if (id == null) return;

        String idShort = textOrNull(n, "idShort");
        String semanticId = textOrNull(n, "semanticId");
        String kind = textOrNull(n, "kind");

        if (idShort == null || semanticId == null || kind == null) {
            try {
                String smIdB64 = toBase64Url(id);
                var headers = headerBuilder.buildMergedHeaders(ss, HttpHeaderBuilder.Mode.READ);
                HttpResponse<Buffer> smResp = traversalClient.getSubmodel(ss.apiUrl, smIdB64, headers).await().indefinitely();
                if (smResp.statusCode() >= 200 && smResp.statusCode() < 300) {
                    JsonNode smNode = objectMapper.readTree(smResp.bodyAsString());
                    if (smNode != null) {
                        if (idShort == null) idShort = textOrNull(smNode, "idShort");
                        if (semanticId == null) semanticId = textOrNull(smNode, "semanticId");
                        if (kind == null) kind = textOrNull(smNode, "kind");
                    }
                }
            } catch (Exception ex) {
                Log.warnf(ex, "Could not enrich submodel %s", id);
            }
        }

        AasSubmodelLite sm = new AasSubmodelLite();
        sm.sourceSystem = ss;
        sm.submodelId = id;
        sm.submodelIdShort = idShort;
        sm.semanticId = semanticId;
        sm.kind = kind;
        sm.persist();
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode f = node.get(field);
        return f != null && !f.isNull() ? f.asText() : null;
    }

    private String toBase64Url(String s) {
        if (s == null) return null;
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private record ZipJson(String name, String lowerName, String json) { }

    private static org.w3c.dom.Element firstChildByLocalName(org.w3c.dom.Element parent, String local) {
        if (parent == null) return null;
        var list = parent.getElementsByTagNameNS("*", local);
        if (list == null || list.getLength() == 0) return null;
        return (org.w3c.dom.Element) list.item(0);
    }

    private static String firstTextByLocalName(org.w3c.dom.Element parent, String local) {
        var el = firstChildByLocalName(parent, local);
        if (el == null) return null;
        var t = el.getTextContent();
        return (t != null && !t.isBlank()) ? t.trim() : null;
    }

    private static java.util.List<org.w3c.dom.Element> childElements(org.w3c.dom.Element parent) {
        java.util.List<org.w3c.dom.Element> out = new java.util.ArrayList<>();
        if (parent == null) return out;
        var nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            var n = nl.item(i);
            if (n instanceof org.w3c.dom.Element e) out.add(e);
        }
        return out;
    }

    private void persistElementLite(AasSubmodelLite submodel, String parentPath, JsonNode n, Set<String> seen) {
        String idShort = textOrNull(n, "idShort");
        if (idShort == null) return;
        String modelType = textOrNull(n, "modelType");
        String idShortPath = parentPath == null || parentPath.isBlank() ? idShort : parentPath + "/" + idShort;

        String key = submodel.id + "::" + idShortPath;
        if (seen != null) {
            if (seen.contains(key)) return;
            seen.add(key);
        }

        AasElementLite e = new AasElementLite();
        e.submodelLite = submodel;
        e.idShort = idShort;
        e.modelType = modelType;
        e.valueType = textOrNull(n, "valueType");
        e.idShortPath = idShortPath;
        e.parentPath = parentPath;
        e.semanticId = textOrNull(n, "semanticId");
        e.typeValueListElement = textOrNull(n, "valueTypeListElement");
        e.orderRelevant = n.has("orderRelevant") ? n.get("orderRelevant").asBoolean(false) : null;
        e.hasChildren = "SubmodelElementCollection".equalsIgnoreCase(modelType)
                || "SubmodelElementList".equalsIgnoreCase(modelType)
                || "Entity".equalsIgnoreCase(modelType);

        if ("Operation".equalsIgnoreCase(modelType)) {
            JsonNode inVars = n.get("inputVariables");
            JsonNode outVars = n.get("outputVariables");
            e.inputSignature = (inVars != null && !inVars.isNull()) ? inVars.toString() : null;
            e.outputSignature = (outVars != null && !outVars.isNull()) ? outVars.toString() : null;
        }
        if ("ReferenceElement".equalsIgnoreCase(modelType)) {
            JsonNode val = n.get("value");
            if (val != null) {
                e.isReference = true;
                e.referenceTargetType = textOrNull(val, "type");
                e.referenceKeys = val.has("keys") ? val.get("keys").toString() : null;
                if (val.has("keys") && val.get("keys").isArray() && val.get("keys").size() > 0) {
                    JsonNode k0 = val.get("keys").get(0);
                    if (k0 != null && "Submodel".equals(textOrNull(k0, "type"))) {
                        e.targetSubmodelId = textOrNull(k0, "value");
                    }
                }
            }
        }
        e.persist();
    }

    @Transactional
    public void applySubmodelCreate(Long sourceSystemId, String submodelJson) {
        var ssOpt = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId);
        if (ssOpt.isEmpty()) return;
        try {
            JsonNode n = objectMapper.readTree(submodelJson);
            persistSubmodelLite(ssOpt.get(), n);
        } catch (Exception e) {
            Log.warnf(e, "Failed to apply submodel create delta for sourceSystemId=%d", sourceSystemId);
        }
    }

    @Transactional
    public void applyElementCreate(Long sourceSystemId, String submodelId, String parentPath, String elementJson) {
        var ssOpt = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId);
        if (ssOpt.isEmpty()) return;
        var submodel = AasSubmodelLite.<AasSubmodelLite>find("sourceSystem.id = ?1 and submodelId = ?2", sourceSystemId, submodelId).firstResult();
        if (submodel == null) return;

        try {
            JsonNode n = objectMapper.readTree(elementJson);
            String idShort = textOrNull(n, "idShort");
            if (idShort == null) return;

            String idShortPath = parentPath == null || parentPath.isBlank() ? idShort : parentPath + "/" + idShort;

            AasElementLite e = new AasElementLite();
            e.submodelLite = submodel;
            e.idShort = idShort;
            e.modelType = textOrNull(n, "modelType");
            e.valueType = textOrNull(n, "valueType");
            e.idShortPath = idShortPath;
            e.parentPath = parentPath;
            e.hasChildren = false;
            e.semanticId = textOrNull(n, "semanticId");
            e.isReference = n.has("isReference") && n.get("isReference").asBoolean(false);
            e.referenceTargetType = textOrNull(n, "referenceTargetType");
            e.referenceKeys = textOrNull(n, "referenceKeys");
            e.targetSubmodelId = textOrNull(n, "targetSubmodelId");
            e.typeValueListElement = textOrNull(n, "typeValueListElement");
            e.orderRelevant = n.has("orderRelevant") ? n.get("orderRelevant").asBoolean(false) : null;
            if ("Operation".equalsIgnoreCase(e.modelType)) {
                JsonNode inVars = n.get("inputVariables");
                JsonNode outVars = n.get("outputVariables");
                e.inputSignature = (inVars != null && !inVars.isNull()) ? inVars.toString() : null;
                e.outputSignature = (outVars != null && !outVars.isNull()) ? outVars.toString() : null;
            } else {
                e.inputSignature = null;
                e.outputSignature = null;
            }
            e.persist();
        } catch (Exception e) {
            Log.warnf(e, "Failed to apply element create delta for sourceSystemId=%d submodelId=%s", sourceSystemId, submodelId);
        }
    }

    @Transactional
    public void applySubmodelDelete(Long sourceSystemId, String submodelId) {
        var submodel = AasSubmodelLite.<AasSubmodelLite>find("sourceSystem.id = ?1 and submodelId = ?2", sourceSystemId, submodelId).firstResult();
        if (submodel == null) return;
        AasElementLite.delete("submodelLite.id", submodel.id);
        submodel.delete();
    }

    @Transactional
    public void applyElementDelete(Long sourceSystemId, String submodelId, String idShortPath) {
        var submodel = AasSubmodelLite.<AasSubmodelLite>find("sourceSystem.id = ?1 and submodelId = ?2", sourceSystemId, submodelId).firstResult();
        if (submodel == null) return;
        AasElementLite.delete("submodelLite.id = ?1 and (idShortPath = ?2 or idShortPath like ?3)", submodel.id, idShortPath, idShortPath + "/%");
    }
}


