package de.unistuttgart.stayinsync.core.configuration.service.aas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasSubmodelLite;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas.AasElementLite;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystem;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.inject.Inject;

import java.util.HashSet;
import java.util.Set;

/**
 * Service responsible for managing and refreshing AAS structure snapshots.
 * Handles the import, parsing, and persistence of submodels and their elements
 * from both live AAS API sources and AASX archive files.
 */
@ApplicationScoped
public class AasStructureSnapshotService {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    AasTraversalClient traversalClient;

    @Inject
    HttpHeaderBuilder headerBuilder;

    /**
     * Initializes the snapshot for a given source system by triggering a full refresh.
     *
     * @param sourceSystemId The ID of the source system whose snapshot should be built.
     */
    @Transactional
    public void buildInitialSnapshot(Long sourceSystemId) {
        refreshSnapshot(sourceSystemId);
    }

    /**
     * Refreshes the AAS snapshot for a specific source system by fetching submodels
     * and elements from the connected AAS API and storing them locally.
     *
     * @param sourceSystemId The ID of the source system to refresh.
     */
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
                    // Deep import to include nested collections/lists/entities
                    Uni<HttpResponse<Buffer>> ue = traversalClient.listElements(ss.apiUrl, smIdB64, "all", null, headers);
                    HttpResponse<Buffer> er = ue.await().indefinitely();
                    if (er.statusCode() >= 200 && er.statusCode() < 300) {
                        String ejson = er.bodyAsString();
                        JsonNode en = objectMapper.readTree(ejson);
                        if (en == null) continue;
                        Set<String> seen = new HashSet<>();
                        if (en.isArray()) {
                            for (JsonNode node : en) {
                                ingestElement(sm, null, node, seen);
                            }
                        } else if (en.has("result") && en.get("result").isArray()) {
                            for (JsonNode node : en.get("result")) {
                                ingestElement(sm, null, node, seen);
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

    /**
     * Parses and imports an AASX archive into the database, extracting submodels
     * and elements from JSON and XML sources.
     *
     * @param sourceSystemId The ID of the source system to which data should be attached.
     * @param filename The original name of the uploaded AASX file.
     * @param fileBytes The raw file contents of the AASX archive.
     * @throws InvalidAasxException If the archive is invalid or cannot be processed.
     */
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
                if (!looksLikeSubmodel) {
                    // IDTA container JSON: iterate submodels array
                    if (root.has("submodels") && root.get("submodels").isArray()) {
                        for (JsonNode smNode : root.get("submodels")) {
                            if (importSubmodelFromJson(ss, smNode)) {
                                foundAnySubmodelPayload = true;
                                importedSubmodels++;
                            }
                        }
                    }
                    continue;
                }
                if (importSubmodelFromJson(ss, root)) {
                    foundAnySubmodelPayload = true;
                    importedSubmodels++;
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

    /**
     * Attaches submodels from an AASX file directly to a live SourceSystem via API calls.
     * Creates and links submodels to the AAS shell upstream.
     *
     * @param sourceSystemId The ID of the source system.
     * @param fileBytes The content of the AASX archive.
     * @return The number of successfully attached submodels.
     */
    @Transactional
    public int attachSubmodelsLive(Long sourceSystemId, byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new InvalidAasxException("Empty file");
        }
        var ssOpt = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId);
        if (ssOpt.isEmpty()) {
            throw new InvalidAasxException("Unknown source system: " + sourceSystemId);
        }
        SourceSystem ss = ssOpt.get();

        int attached = 0;

        // Read entries and JSON parts (while tracking if archive had any entries at all)
        var zipRead = readAasxJsonEntries(fileBytes);
        if (!zipRead.hasEntries) {
            throw new InvalidAasxException("Not a valid AASX: archive has no entries");
        }

        Log.infof("Live attach: discovered %d JSON entries in AASX", zipRead.jsonEntries.size());

        var headersWrite = headerBuilder.buildMergedHeaders(ss, HttpHeaderBuilder.Mode.WRITE_JSON);
        var headersRead = headerBuilder.buildMergedHeaders(ss, HttpHeaderBuilder.Mode.READ);

        // Process JSON entries
        for (ZipJson zj : zipRead.jsonEntries) {
            try {
                Log.debugf("Live attach: scanning JSON entry '%s'", zj.name());
                JsonNode root = objectMapper.readTree(zj.json);
                if (root == null) continue;

                var candidates = getSubmodelCandidatesFromJson(root);
                Log.infof("Live attach: JSON entry '%s' produced %d candidate submodels", zj.name(), candidates.size());
                for (JsonNode smNode : candidates) {
                    String id = textOrNull(smNode, "id");
                    if (id == null && smNode.has("identification")) {
                        id = textOrNull(smNode.get("identification"), "id");
                    }
                    if (id == null || id.isBlank()) continue;

                    Log.infof("Live attach: processing submodel id=%s from JSON", id);
                    // If exists upstream, ensure shell reference
                    if (checkAndAttachExisting(ss, id, headersWrite, headersRead)) {
                        attached++;
                        continue;
                    }
                    // Otherwise create and attach
                    String createdId = createSubmodelUpstreamFromJson(ss, smNode, headersWrite);
                    if (createdId != null && tryAddSubmodelRef(ss, createdId, headersWrite, headersRead)) {
                        attached++;
                    }
                }
            } catch (Exception ex) {
                Log.warn("Skipping JSON entry from AASX during live attach", ex);
            }
        }

        // XML fallback
        if (attached == 0) {
            attached += attachSubmodelsFromXmlFallback(ss, fileBytes, headersWrite, headersRead);
        }

        return attached;
    }

    private static final class ZipReadResult {
        final java.util.List<ZipJson> jsonEntries;
        final boolean hasEntries;
        ZipReadResult(java.util.List<ZipJson> jsonEntries, boolean hasEntries) {
            this.jsonEntries = jsonEntries;
            this.hasEntries = hasEntries;
        }
    }

    private ZipReadResult readAasxJsonEntries(byte[] fileBytes) {
        final java.util.List<ZipJson> jsonEntries = new java.util.ArrayList<>();
        boolean hasEntries = false;
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fileBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null) continue;
                hasEntries = true;
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                if (!lower.endsWith(".json")) continue;
                try {
                    String json = new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    jsonEntries.add(new ZipJson(name, lower, json));
                } catch (Exception ignore) { }
            }
        } catch (java.io.IOException e) {
            throw new InvalidAasxException("Failed to read AASX: " + e.getMessage());
        }
        return new ZipReadResult(jsonEntries, hasEntries);
    }

    private java.util.List<JsonNode> getSubmodelCandidatesFromJson(JsonNode root) {
        java.util.List<JsonNode> candidates = new java.util.ArrayList<>();
        if (root == null) return candidates;
        if ((root.has("modelType") && "Submodel".equalsIgnoreCase(textOrNull(root, "modelType")))
                || (root.has("submodelElements") && root.get("submodelElements").isArray())) {
            candidates.add(root);
        }
        if (root.has("submodel")) {
            candidates.add(root.get("submodel"));
        }
        if (root.has("submodels") && root.get("submodels").isArray()) {
            for (JsonNode sm : root.get("submodels")) candidates.add(sm);
        }
        return candidates;
    }

    private boolean checkAndAttachExisting(SourceSystem ss, String id, java.util.Map<String,String> headersWrite, java.util.Map<String,String> headersRead) {
        try {
            String smIdB64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            io.vertx.mutiny.ext.web.client.HttpResponse<io.vertx.mutiny.core.buffer.Buffer> check = traversalClient.getSubmodel(ss.apiUrl, smIdB64, headersRead).await().indefinitely();
            if (check.statusCode() >= 200 && check.statusCode() < 300) {
                return tryAddSubmodelRef(ss, id, headersWrite, headersRead);
            }
        } catch (Exception ignore) { }
        return false;
    }

    private String createSubmodelUpstreamFromJson(SourceSystem ss, JsonNode smNode, java.util.Map<String,String> headersWrite) {
        String id = textOrNull(smNode, "id");
        if (id == null && smNode.has("identification")) {
            id = textOrNull(smNode.get("identification"), "id");
        }
        String payload = buildNormalizedSubmodelPayload(smNode);
        Log.debugf("Live attach: creating submodel id=%s payload=%s", id, payload);
        var createResp = traversalClient.createSubmodel(ss.apiUrl, payload, headersWrite).await().indefinitely();
        if (createResp.statusCode() >= 200 && createResp.statusCode() < 300) {
            String createdId = id;
            try {
                String b = createResp.bodyAsString();
                if (b != null && !b.isBlank() && b.trim().startsWith("{")) {
                    JsonNode created = objectMapper.readTree(b);
                    String cid = textOrNull(created, "id");
                    if (cid != null && !cid.isBlank()) createdId = cid;
                }
            } catch (Exception ignore) { }
            Log.infof("Live attach: created submodel upstream id=%s (from JSON)", createdId);
            return createdId;
        } else {
            Log.warnf("Failed to create submodel id=%s upstream: %d %s", id, createResp.statusCode(), createResp.statusMessage());
            return null;
        }
    }

    private int attachSubmodelsFromXmlFallback(SourceSystem ss, byte[] fileBytes, java.util.Map<String,String> headersWrite, java.util.Map<String,String> headersRead) {
        int attached = 0;
        Log.info("Live attach: No submodels attached from JSON, trying XML fallback");
        try (java.util.zip.ZipInputStream zis2 = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fileBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis2.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null) continue;
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                if (!lower.endsWith(".xml")) continue;
                try {
                    Log.debugf("Live attach: scanning XML entry '%s'", name);
                    javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                    dbf.setNamespaceAware(true);
                    javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
                    org.w3c.dom.Document doc = db.parse(zis2);
                    org.w3c.dom.Element root = doc.getDocumentElement();
                    if (root == null) continue;
                    String rootLocal = root.getLocalName() != null ? root.getLocalName() : root.getNodeName();
                    if (rootLocal == null || !rootLocal.toLowerCase(java.util.Locale.ROOT).contains("submodel")) {
                        var subNodes = root.getElementsByTagNameNS("*", "submodel");
                        if (subNodes.getLength() > 0) {
                            root = (org.w3c.dom.Element) subNodes.item(0);
                        }
                    }
                    String id = firstTextByLocalName(root, "id");
                    if (id == null) {
                        var ident = firstChildByLocalName(root, "identification");
                        if (ident != null) id = firstTextByLocalName(ident, "id");
                        if (id == null) {
                            var identifier = firstChildByLocalName(root, "identifier");
                            if (identifier != null) {
                                String i1 = identifier.getAttribute("id");
                                if (i1 != null && !i1.isBlank()) id = i1;
                                if (id == null) id = identifier.getTextContent();
                            }
                        }
                    }
                    if (id == null || id.isBlank()) {
                        id = name;
                    }
                    Log.infof("Live attach: processing submodel id=%s from XML", id);
                    // Skip if already present
                    try {
                        String smIdB64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        var check = traversalClient.getSubmodel(ss.apiUrl, smIdB64, headersRead).await().indefinitely();
                        if (check.statusCode() >= 200 && check.statusCode() < 300) {
                            if (tryAddSubmodelRef(ss, id, headersWrite, headersRead)) attached++;
                            continue;
                        }
                    } catch (Exception ignore) { }

                    String idShort = firstTextByLocalName(root, "idShort");
                    com.fasterxml.jackson.databind.node.ObjectNode out = objectMapper.createObjectNode();
                    out.put("modelType", "Submodel");
                    out.put("id", id);
                    if (idShort != null) out.put("idShort", idShort);
                    out.putArray("submodelElements");
                    var createResp = traversalClient.createSubmodel(ss.apiUrl, out.toString(), headersWrite).await().indefinitely();
                    if (createResp.statusCode() >= 200 && createResp.statusCode() < 300) {
                        String createdId = id;
                        try {
                            String b = createResp.bodyAsString();
                            if (b != null && !b.isBlank() && b.trim().startsWith("{")) {
                                JsonNode created = objectMapper.readTree(b);
                                String cid = textOrNull(created, "id");
                                if (cid != null && !cid.isBlank()) createdId = cid;
                            }
                        } catch (Exception ignore) { }
                        Log.infof("Live attach: created submodel upstream id=%s (from XML)", createdId);
                        if (tryAddSubmodelRef(ss, createdId, headersWrite, headersRead)) attached++;
                    } else {
                        Log.warnf("Failed to create submodel from XML id=%s upstream: %d %s", id, createResp.statusCode(), createResp.statusMessage());
                    }
                } catch (Exception ignore) {
                }
            }
        } catch (java.io.IOException e) {
            // ignore
        }
        return attached;
    }

    /**
     * Same as {@link #attachSubmodelsLive(Long, byte[])} but operates on a TargetSystem.
     * Uploads and attaches submodels directly to the target system’s AAS shell.
     *
     * @param targetSystemId The ID of the target system.
     * @param fileBytes The content of the AASX archive.
     * @return The number of successfully attached submodels.
     */
    @Transactional
    public int attachSubmodelsLiveToTarget(Long targetSystemId, byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new InvalidAasxException("Empty file");
        }
        var tsOpt = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId);
        if (tsOpt.isEmpty()) {
            throw new InvalidAasxException("Unknown target system: " + targetSystemId);
        }
        TargetSystem ts = tsOpt.get();

        int attached = 0;
        final java.util.List<ZipJson> jsonEntries = new java.util.ArrayList<>();
        boolean hasEntries = false;
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fileBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null) continue;
                hasEntries = true;
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                if (!lower.endsWith(".json")) continue;
                try {
                    String json = new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    jsonEntries.add(new ZipJson(name, lower, json));
                } catch (Exception ignore) { }
            }
        } catch (java.io.IOException e) {
            throw new InvalidAasxException("Failed to read AASX: " + e.getMessage());
        }
        if (!hasEntries) {
            throw new InvalidAasxException("Not a valid AASX: archive has no entries");
        }

        Log.infof("Live attach (target): discovered %d JSON entries in AASX", jsonEntries.size());

        var headersWrite = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.WRITE_JSON);
        var headersRead = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.READ);

        for (ZipJson zj : jsonEntries) {
            try {
                JsonNode root = objectMapper.readTree(zj.json);
                if (root == null) continue;
                java.util.List<JsonNode> candidates = new java.util.ArrayList<>();
                if ((root.has("modelType") && "Submodel".equalsIgnoreCase(textOrNull(root, "modelType")))
                        || (root.has("submodelElements") && root.get("submodelElements").isArray())) {
                    candidates.add(root);
                }
                if (root.has("submodel")) candidates.add(root.get("submodel"));
                if (root.has("submodels") && root.get("submodels").isArray()) {
                    for (JsonNode sm : root.get("submodels")) candidates.add(sm);
                }
                for (JsonNode smNode : candidates) {
                    String id = textOrNull(smNode, "id");
                    if (id == null && smNode.has("identification")) id = textOrNull(smNode.get("identification"), "id");
                    if (id == null || id.isBlank()) continue;
                    Log.infof("attachSubmodelsLiveToTarget: candidate id=%s (JSON)", id);
                    try {
                        String smIdB64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        var check = traversalClient.getSubmodel(ts.apiUrl, smIdB64, headersRead).await().indefinitely();
                        Log.infof("attachSubmodelsLiveToTarget: GET submodel check status=%d", check.statusCode());
                        if (check.statusCode() >= 200 && check.statusCode() < 300) {
                            if (tryAddSubmodelRef(ts, id, headersWrite, headersRead)) attached++;
                            continue;
                        }
                    } catch (Exception ignore) { }
                    String payload = buildNormalizedSubmodelPayload(smNode);
                    Log.debugf("attachSubmodelsLiveToTarget: create payload=%s", payload);
                    var createResp = traversalClient.createSubmodel(ts.apiUrl, payload, headersWrite).await().indefinitely();
                    Log.infof("attachSubmodelsLiveToTarget: create status=%d", createResp.statusCode());
                    if (createResp.statusCode() >= 200 && createResp.statusCode() < 300) {
                        String createdId = id;
                        try {
                            String b = createResp.bodyAsString();
                            if (b != null && !b.isBlank() && b.trim().startsWith("{")) {
                                JsonNode created = objectMapper.readTree(b);
                                String cid = textOrNull(created, "id");
                                if (cid != null && !cid.isBlank()) createdId = cid;
                            }
                        } catch (Exception ignore) { }
                        boolean added = tryAddSubmodelRef(ts, createdId, headersWrite, headersRead);
                        Log.infof("attachSubmodelsLiveToTarget: add-ref result=%s", added);
                        if (added) attached++;
                    }
                }
            } catch (Exception ignore) { }
        }
        if (attached == 0) {
            Log.info("Live attach (target): No submodels attached from JSON, trying XML fallback");
            try (java.util.zip.ZipInputStream zis2 = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fileBytes))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis2.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name == null) continue;
                    String lower = name.toLowerCase(java.util.Locale.ROOT);
                    if (!lower.endsWith(".xml")) continue;
                    try {
                        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                        dbf.setNamespaceAware(true);
                        javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
                        org.w3c.dom.Document doc = db.parse(zis2);
                        org.w3c.dom.Element root = doc.getDocumentElement();
                        if (root == null) continue;
                        String rootLocal = root.getLocalName() != null ? root.getLocalName() : root.getNodeName();
                        if (rootLocal == null || !rootLocal.toLowerCase(java.util.Locale.ROOT).contains("submodel")) {
                            var subNodes = root.getElementsByTagNameNS("*", "submodel");
                            if (subNodes.getLength() > 0) {
                                root = (org.w3c.dom.Element) subNodes.item(0);
                            }
                        }
                        String id = firstTextByLocalName(root, "id");
                        if (id == null) {
                            var ident = firstChildByLocalName(root, "identification");
                            if (ident != null) id = firstTextByLocalName(ident, "id");
                            if (id == null) {
                                var identifier = firstChildByLocalName(root, "identifier");
                                if (identifier != null) {
                                    String i1 = identifier.getAttribute("id");
                                    if (i1 != null && !i1.isBlank()) id = i1;
                                    if (id == null) id = identifier.getTextContent();
                                }
                            }
                        }
                        if (id == null || id.isBlank()) id = name;
                        try {
                            String smIdB64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            var check = traversalClient.getSubmodel(ts.apiUrl, smIdB64, headersRead).await().indefinitely();
                            Log.infof("attachSubmodelsLiveToTarget(XML): GET submodel check status=%d", check.statusCode());
                            if (check.statusCode() >= 200 && check.statusCode() < 300) {
                                if (tryAddSubmodelRef(ts, id, headersWrite, headersRead)) attached++;
                                continue;
                            }
                        } catch (Exception ignore) { }
                        String idShort = firstTextByLocalName(root, "idShort");
                        com.fasterxml.jackson.databind.node.ObjectNode out = objectMapper.createObjectNode();
                        out.put("modelType", "Submodel");
                        out.put("id", id);
                        if (idShort != null) out.put("idShort", idShort);
                        out.putArray("submodelElements");
                        var createResp = traversalClient.createSubmodel(ts.apiUrl, out.toString(), headersWrite).await().indefinitely();
                        Log.infof("attachSubmodelsLiveToTarget(XML): create status=%d", createResp.statusCode());
                        if (createResp.statusCode() >= 200 && createResp.statusCode() < 300) {
                            String createdId = id;
                            try {
                                String b = createResp.bodyAsString();
                                if (b != null && !b.isBlank() && b.trim().startsWith("{")) {
                                    JsonNode created = objectMapper.readTree(b);
                                    String cid = textOrNull(created, "id");
                                    if (cid != null && !cid.isBlank()) createdId = cid;
                                }
                            } catch (Exception ignore) { }
                            boolean added = tryAddSubmodelRef(ts, createdId, headersWrite, headersRead);
                            Log.infof("attachSubmodelsLiveToTarget(XML): add-ref result=%s", added);
                            if (added) attached++;
                        } else {
                            Log.warnf("Live attach (target): failed to create submodel from XML id=%s upstream: %d %s", id, createResp.statusCode(), createResp.statusMessage());
                        }
                    } catch (Exception ignore) { }
                }
            } catch (java.io.IOException e) {
                // ignore
            }
        }
        return attached;
    }

    /**
     * Builds a lightweight JSON preview of attachable items from an AASX file.
     * Used to show submodels and their elements before attaching them.
     *
     * @param fileBytes The content of the AASX archive.
     * @return A JSON object describing available submodels and elements.
     */
    public io.vertx.core.json.JsonObject previewAasx(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new InvalidAasxException("Empty file");
        }
        io.vertx.core.json.JsonArray submodels = new io.vertx.core.json.JsonArray();
        final java.util.List<ZipJson> jsonEntries = new java.util.ArrayList<>();
        boolean hasEntries = false;
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fileBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null) continue;
                hasEntries = true;
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                if (lower.endsWith(".json")) {
                    try {
                        String json = new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        jsonEntries.add(new ZipJson(name, lower, json));
                    } catch (Exception ignore) { }
                }
            }
        } catch (java.io.IOException e) {
            throw new InvalidAasxException("Failed to read AASX: " + e.getMessage());
        }
        if (!hasEntries) {
            throw new InvalidAasxException("Not a valid AASX: archive has no entries");
        }

        // Prefer JSON entries
        for (ZipJson zj : jsonEntries) {
            try {
                JsonNode root = objectMapper.readTree(zj.json);
                if (root == null) continue;
                java.util.List<JsonNode> candidates = new java.util.ArrayList<>();
                if ((root.has("modelType") && "Submodel".equalsIgnoreCase(textOrNull(root, "modelType")))
                        || (root.has("submodelElements") && root.get("submodelElements").isArray())) {
                    candidates.add(root);
                }
                if (root.has("submodel")) candidates.add(root.get("submodel"));
                if (root.has("submodels") && root.get("submodels").isArray()) {
                    for (JsonNode sm : root.get("submodels")) candidates.add(sm);
                }
                for (JsonNode smNode : candidates) {
                    String id = textOrNull(smNode, "id");
                    if (id == null && smNode.has("identification")) id = textOrNull(smNode.get("identification"), "id");
                    if (id == null || id.isBlank()) continue;
                    io.vertx.core.json.JsonObject sm = new io.vertx.core.json.JsonObject();
                    sm.put("id", id);
                    sm.put("idShort", textOrNull(smNode, "idShort"));
                    sm.put("kind", textOrNull(smNode, "kind"));
                    sm.put("from", "json");
                    io.vertx.core.json.JsonArray elements = new io.vertx.core.json.JsonArray();
                    JsonNode els = smNode.get("submodelElements");
                    if (els != null && els.isArray()) {
                        for (JsonNode el : els) {
                            String mt = textOrNull(el, "modelType");
                            String idShort = textOrNull(el, "idShort");
                            if (idShort == null) continue;
                            if (mt == null) mt = "";
                            // Suggest only collections/lists by default
                            if (mt.equalsIgnoreCase("SubmodelElementCollection") || mt.equalsIgnoreCase("SubmodelElementList")) {
                                elements.add(new io.vertx.core.json.JsonObject().put("idShort", idShort).put("modelType", mt));
                            }
                        }
                    }
                    sm.put("elements", elements);
                    submodels.add(sm);
                }
            } catch (Exception ignore) { }
        }

        // If no JSON candidates found, try to extract minimal preview from XML
        if (submodels.isEmpty()) {
            try (java.util.zip.ZipInputStream zis2 = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fileBytes))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zis2.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name == null) continue;
                    String lower = name.toLowerCase(java.util.Locale.ROOT);
                    if (!lower.endsWith(".xml")) continue;
                    try {
                        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                        dbf.setNamespaceAware(true);
                        javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
                        org.w3c.dom.Document doc = db.parse(zis2);
                        org.w3c.dom.Element root = doc.getDocumentElement();
                        if (root == null) continue;
                        String rootLocal = root.getLocalName() != null ? root.getLocalName() : root.getNodeName();
                        boolean subFound = (rootLocal != null && rootLocal.toLowerCase(java.util.Locale.ROOT).contains("submodel"));
                        if (!subFound) {
                            var subNodes = root.getElementsByTagNameNS("*", "submodel");
                            if (subNodes.getLength() > 0) {
                                root = (org.w3c.dom.Element) subNodes.item(0);
                                subFound = true;
                            }
                        }
                        if (!subFound) continue; // skip XML entries that are not submodels (e.g. [Content_Types].xml)
                        String id = firstTextByLocalName(root, "id");
                        if (id == null) {
                            var ident = firstChildByLocalName(root, "identification");
                            if (ident != null) id = firstTextByLocalName(ident, "id");
                            if (id == null) {
                                var identifier = firstChildByLocalName(root, "identifier");
                                if (identifier != null) {
                                    String i1 = identifier.getAttribute("id");
                                    if (i1 != null && !i1.isBlank()) id = i1;
                                    if (id == null) id = identifier.getTextContent();
                                }
                            }
                        }
                        if (id == null || id.isBlank()) id = name;
                        io.vertx.core.json.JsonObject sm = new io.vertx.core.json.JsonObject();
                        sm.put("id", id);
                        sm.put("idShort", firstTextByLocalName(root, "idShort"));
                        sm.put("kind", firstTextByLocalName(root, "kind"));
                        sm.put("from", "xml");
                        io.vertx.core.json.JsonArray elements = new io.vertx.core.json.JsonArray();
                        var smEls = firstChildByLocalName(root, "submodelElements");
                        if (smEls == null) smEls = firstChildByLocalName(root, "subModelElements");
                        if (smEls != null) {
                            var children = childElements(smEls);
                            for (org.w3c.dom.Element el : children) {
                                String elIdShort = firstTextByLocalName(el, "idShort");
                                if (elIdShort == null) continue;
                                String modelType = el.getLocalName() != null ? el.getLocalName() : el.getNodeName();
                                String ml = modelType != null ? modelType.toLowerCase(java.util.Locale.ROOT) : "";
                                if (ml.contains("submodelelementcollection")) modelType = "SubmodelElementCollection";
                                else if (ml.contains("submodelelementlist")) modelType = "SubmodelElementList";
                                if ("SubmodelElementCollection".equals(modelType) || "SubmodelElementList".equals(modelType)) {
                                    elements.add(new io.vertx.core.json.JsonObject().put("idShort", elIdShort).put("modelType", modelType));
                                }
                            }
                        }
                        sm.put("elements", elements);
                        submodels.add(sm);
                    } catch (Exception ignore) { }
                }
            } catch (java.io.IOException e) {
                // ignore
            }
        }

        return new io.vertx.core.json.JsonObject().put("submodels", submodels);
    }

    /**
     * Attaches only selected submodels or elements from an AASX to a SourceSystem.
     *
     * @param sourceSystemId The ID of the source system.
     * @param fileBytes The content of the AASX archive.
     * @param selection JSON structure specifying which submodels or elements to attach.
     * @return The number of successfully attached items.
     */
    @Transactional
    public int attachSelectedFromAasx(Long sourceSystemId, byte[] fileBytes, io.vertx.core.json.JsonObject selection) {
        if (fileBytes == null || fileBytes.length == 0) throw new InvalidAasxException("Empty file");
        var ssOpt = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId);
        if (ssOpt.isEmpty()) throw new InvalidAasxException("Unknown source system: " + sourceSystemId);
        SourceSystem ss = ssOpt.get();

        java.util.Map<String, JsonNode> submodelById = new java.util.HashMap<>();
        java.util.Map<String, org.w3c.dom.Element> xmlSubmodelById = new java.util.HashMap<>();
        final java.util.List<ZipJson> jsonEntries = new java.util.ArrayList<>();
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fileBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null) continue;
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                if (!lower.endsWith(".json")) continue;
                try {
                    String json = new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    jsonEntries.add(new ZipJson(name, lower, json));
                } catch (Exception ignore) { }
            }
        } catch (java.io.IOException e) {
            throw new InvalidAasxException("Failed to read AASX: " + e.getMessage());
        }
        for (ZipJson zj : jsonEntries) {
            try {
                JsonNode root = objectMapper.readTree(zj.json);
                if (root == null) continue;
                java.util.List<JsonNode> candidates = new java.util.ArrayList<>();
                if ((root.has("modelType") && "Submodel".equalsIgnoreCase(textOrNull(root, "modelType")))
                        || (root.has("submodelElements") && root.get("submodelElements").isArray())) {
                    candidates.add(root);
                }
                if (root.has("submodel")) candidates.add(root.get("submodel"));
                if (root.has("submodels") && root.get("submodels").isArray()) {
                    for (JsonNode sm : root.get("submodels")) candidates.add(sm);
                }
                for (JsonNode smNode : candidates) {
                    String id = textOrNull(smNode, "id");
                    if (id == null && smNode.has("identification")) id = textOrNull(smNode.get("identification"), "id");
                    if (id == null || id.isBlank()) continue;
                    submodelById.put(id, smNode);
                }
            } catch (Exception ignore) { }
        }

        // Build XML index for fallback (when JSON not available)
        try (java.util.zip.ZipInputStream zis2 = new java.util.zip.ZipInputStream(new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fileBytes)))) {
            // nested ZipInputStream to avoid interfering with outer stream position
        } catch (Exception ignore) { }
        try (java.util.zip.ZipInputStream zis3 = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fileBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis3.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null) continue;
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                if (!lower.endsWith(".xml")) continue;
                try {
                    javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                    dbf.setNamespaceAware(true);
                    javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
                    org.w3c.dom.Document doc = db.parse(zis3);
                    org.w3c.dom.Element root = doc.getDocumentElement();
                    if (root == null) continue;
                    String rootLocal = root.getLocalName() != null ? root.getLocalName() : root.getNodeName();
                    boolean subFound = (rootLocal != null && rootLocal.toLowerCase(java.util.Locale.ROOT).contains("submodel"));
                    if (!subFound) {
                        var subNodes = root.getElementsByTagNameNS("*", "submodel");
                        if (subNodes.getLength() > 0) {
                            root = (org.w3c.dom.Element) subNodes.item(0);
                            subFound = true;
                        }
                    }
                    if (!subFound) continue;
                    String id = firstTextByLocalName(root, "id");
                    if (id == null) {
                        var ident = firstChildByLocalName(root, "identification");
                        if (ident != null) id = firstTextByLocalName(ident, "id");
                        if (id == null) {
                            var identifier = firstChildByLocalName(root, "identifier");
                            if (identifier != null) {
                                String i1 = identifier.getAttribute("id");
                                if (i1 != null && !i1.isBlank()) id = i1;
                                if (id == null) id = identifier.getTextContent();
                            }
                        }
                    }
                    if (id == null || id.isBlank()) id = name;
                    xmlSubmodelById.put(id, root);
                } catch (Exception ignore) { }
            }
        } catch (java.io.IOException ignore) { }

        var headersWrite = headerBuilder.buildMergedHeaders(ss, HttpHeaderBuilder.Mode.WRITE_JSON);
        var headersRead = headerBuilder.buildMergedHeaders(ss, HttpHeaderBuilder.Mode.READ);
        int attached = 0;
        io.vertx.core.json.JsonArray subs = selection != null ? selection.getJsonArray("submodels", new io.vertx.core.json.JsonArray()) : new io.vertx.core.json.JsonArray();
        for (int i = 0; i < subs.size(); i++) {
            io.vertx.core.json.JsonObject sel = subs.getJsonObject(i);
            if (sel == null) continue;
            String id = sel.getString("id");
            if (id == null || id.isBlank()) continue;
            boolean full = sel.getBoolean("full", false);
            java.util.Set<String> wantedEls = new java.util.HashSet<>();
            io.vertx.core.json.JsonArray elArr = sel.getJsonArray("elements");
            if (elArr != null) {
                for (int j = 0; j < elArr.size(); j++) {
                    String p = elArr.getString(j);
                    if (p != null && !p.isBlank()) wantedEls.add(p);
                }
            }

            JsonNode smNode = submodelById.get(id);
            boolean usedJson = (smNode != null);
            org.w3c.dom.Element xmlRoot = usedJson ? null : xmlSubmodelById.get(id);
            if (!usedJson && xmlRoot == null) {
                Log.warnf("attachSelected: submodel id=%s not found in JSON or XML entries, skipping", id);
                continue;
            }

            String payload;
            if (usedJson) {
                if (full || wantedEls.isEmpty()) {
                    payload = buildNormalizedSubmodelPayload(smNode);
                } else {
                    // Build filtered submodel payload from JSON
                    com.fasterxml.jackson.databind.node.ObjectNode out = objectMapper.createObjectNode();
                    String idShort = textOrNull(smNode, "idShort");
                    out.put("modelType", "Submodel");
                    if (id != null) out.put("id", id);
                    if (idShort != null) out.put("idShort", idShort);
                    com.fasterxml.jackson.databind.node.ArrayNode arrOut = objectMapper.createArrayNode();
                    JsonNode els = smNode.get("submodelElements");
                    if (els != null && els.isArray()) {
                        for (JsonNode el : els) {
                            String idShortEl = textOrNull(el, "idShort");
                            if (idShortEl == null) continue;
                            if (!wantedEls.contains(idShortEl)) continue;
                            String mt = textOrNull(el, "modelType");
                            if (mt == null || !(mt.equalsIgnoreCase("SubmodelElementCollection") || mt.equalsIgnoreCase("SubmodelElementList"))) {
                                continue;
                            }
                            arrOut.add(el);
                        }
                    }
                    out.set("submodelElements", arrOut);
                    payload = out.toString();
                }
            } else {
                // XML fallback: we can only create minimal submodel; element-level filtering from XML is not yet supported
                String idShort = firstTextByLocalName(xmlRoot, "idShort");
                com.fasterxml.jackson.databind.node.ObjectNode out = objectMapper.createObjectNode();
                out.put("modelType", "Submodel");
                if (id != null) out.put("id", id);
                if (idShort != null) out.put("idShort", idShort);
                out.putArray("submodelElements");
                payload = out.toString();
                if (!full && !wantedEls.isEmpty()) {
                    Log.infof("attachSelected: XML-only submodel id=%s selected with elements, creating empty submodel (elements not available from XML)", id);
                }
            }

            // Skip if already exists upstream; if exists, ensure ref
            try {
                String smIdB64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                var check = traversalClient.getSubmodel(ss.apiUrl, smIdB64, headersRead).await().indefinitely();
                if (check.statusCode() >= 200 && check.statusCode() < 300) {
                    if (tryAddSubmodelRef(ss, id, headersWrite, headersRead)) attached++;
                    continue;
                }
            } catch (Exception ignore) { }

            var createResp = traversalClient.createSubmodel(ss.apiUrl, payload, headersWrite).await().indefinitely();
            if (createResp.statusCode() >= 200 && createResp.statusCode() < 300) {
                String createdId = id;
                try {
                    String b = createResp.bodyAsString();
                    if (b != null && !b.isBlank() && b.trim().startsWith("{")) {
                        JsonNode created = objectMapper.readTree(b);
                        String cid = textOrNull(created, "id");
                        if (cid != null && !cid.isBlank()) createdId = cid;
                    }
                } catch (Exception ignore) { }
                if (tryAddSubmodelRef(ss, createdId, headersWrite, headersRead)) attached++;
            } else {
                Log.warnf("attachSelected: failed to create submodel id=%s upstream: %d %s", id, createResp.statusCode(), createResp.statusMessage());
            }
        }
        return attached;
    }

    /**
     * Attaches only selected submodels or elements from an AASX to a TargetSystem.
     *
     * @param targetSystemId The ID of the target system.
     * @param fileBytes The content of the AASX archive.
     * @param selection JSON structure specifying which submodels or elements to attach.
     * @return The number of successfully attached items.
     */
    @Transactional
    public int attachSelectedFromAasxToTarget(Long targetSystemId, byte[] fileBytes, io.vertx.core.json.JsonObject selection) {
        if (fileBytes == null || fileBytes.length == 0) throw new InvalidAasxException("Empty file");
        var tsOpt = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId);
        if (tsOpt.isEmpty()) throw new InvalidAasxException("Unknown target system: " + targetSystemId);
        TargetSystem ts = tsOpt.get();

        java.util.Map<String, JsonNode> submodelById = new java.util.HashMap<>();
        final java.util.List<ZipJson> jsonEntries = new java.util.ArrayList<>();
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fileBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null) continue;
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                if (!lower.endsWith(".json")) continue;
                try {
                    String json = new String(zis.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    jsonEntries.add(new ZipJson(name, lower, json));
                } catch (Exception ignore) { }
            }
        } catch (java.io.IOException e) {
            throw new InvalidAasxException("Failed to read AASX: " + e.getMessage());
        }
        for (ZipJson zj : jsonEntries) {
            try {
                JsonNode root = objectMapper.readTree(zj.json);
                if (root == null) continue;
                java.util.List<JsonNode> candidates = new java.util.ArrayList<>();
                if ((root.has("modelType") && "Submodel".equalsIgnoreCase(textOrNull(root, "modelType")))
                        || (root.has("submodelElements") && root.get("submodelElements").isArray())) {
                    candidates.add(root);
                }
                if (root.has("submodel")) candidates.add(root.get("submodel"));
                if (root.has("submodels") && root.get("submodels").isArray()) {
                    for (JsonNode sm : root.get("submodels")) candidates.add(sm);
                }
                for (JsonNode smNode : candidates) {
                    String id = textOrNull(smNode, "id");
                    if (id == null && smNode.has("identification")) id = textOrNull(smNode.get("identification"), "id");
                    if (id == null || id.isBlank()) continue;
                    submodelById.put(id, smNode);
                }
            } catch (Exception ignore) { }
        }

        // Build XML index for fallback (when JSON is not available)
        java.util.Map<String, org.w3c.dom.Element> xmlSubmodelById = new java.util.HashMap<>();
        try (java.util.zip.ZipInputStream zis2 = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fileBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis2.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null) continue;
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                if (!lower.endsWith(".xml")) continue;
                try {
                    javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                    dbf.setNamespaceAware(true);
                    javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
                    org.w3c.dom.Document doc = db.parse(zis2);
                    org.w3c.dom.Element root = doc.getDocumentElement();
                    if (root == null) continue;
                    String rootLocal = root.getLocalName() != null ? root.getLocalName() : root.getNodeName();
                    boolean subFound = (rootLocal != null && rootLocal.toLowerCase(java.util.Locale.ROOT).contains("submodel"));
                    if (!subFound) {
                        var subNodes = root.getElementsByTagNameNS("*", "submodel");
                        if (subNodes.getLength() > 0) {
                            root = (org.w3c.dom.Element) subNodes.item(0);
                            subFound = true;
                        }
                    }
                    if (!subFound) continue;
                    String id = firstTextByLocalName(root, "id");
                    if (id == null) {
                        var ident = firstChildByLocalName(root, "identification");
                        if (ident != null) id = firstTextByLocalName(ident, "id");
                        if (id == null) {
                            var identifier = firstChildByLocalName(root, "identifier");
                            if (identifier != null) {
                                String i1 = identifier.getAttribute("id");
                                if (i1 != null && !i1.isBlank()) id = i1;
                                if (id == null) id = identifier.getTextContent();
                            }
                        }
                    }
                    if (id == null || id.isBlank()) id = name;
                    xmlSubmodelById.put(id, root);
                } catch (Exception ignore) { }
            }
        } catch (java.io.IOException ignore) { }

        var headersWrite = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.WRITE_JSON);
        var headersRead = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.READ);
        int attached = 0;
        io.vertx.core.json.JsonArray subs = selection != null ? selection.getJsonArray("submodels", new io.vertx.core.json.JsonArray()) : new io.vertx.core.json.JsonArray();
        for (int i = 0; i < subs.size(); i++) {
            io.vertx.core.json.JsonObject sel = subs.getJsonObject(i);
            if (sel == null) continue;
            String id = sel.getString("id");
            if (id == null || id.isBlank()) continue;
            boolean full = sel.getBoolean("full", false);
            java.util.Set<String> wantedEls = new java.util.HashSet<>();
            io.vertx.core.json.JsonArray elArr = sel.getJsonArray("elements");
            if (elArr != null) {
                for (int j = 0; j < elArr.size(); j++) {
                    String p = elArr.getString(j);
                    if (p != null && !p.isBlank()) wantedEls.add(p);
                }
            }

            JsonNode smNode = submodelById.get(id);
            String payload;
            if (smNode != null) {
                if (full || wantedEls.isEmpty()) {
                    payload = buildNormalizedSubmodelPayload(smNode);
                } else {
                    com.fasterxml.jackson.databind.node.ObjectNode out = objectMapper.createObjectNode();
                    String idShort = textOrNull(smNode, "idShort");
                    out.put("modelType", "Submodel");
                    if (id != null) out.put("id", id);
                    if (idShort != null) out.put("idShort", idShort);
                    com.fasterxml.jackson.databind.node.ArrayNode arrOut = objectMapper.createArrayNode();
                    JsonNode els = smNode.get("submodelElements");
                    if (els != null && els.isArray()) {
                        for (JsonNode el : els) {
                            String idShortEl = textOrNull(el, "idShort");
                            if (idShortEl == null) continue;
                            if (!wantedEls.contains(idShortEl)) continue;
                            String mt = textOrNull(el, "modelType");
                            if (mt == null || !(mt.equalsIgnoreCase("SubmodelElementCollection") || mt.equalsIgnoreCase("SubmodelElementList"))) {
                                continue;
                            }
                            arrOut.add(el);
                        }
                    }
                    out.set("submodelElements", arrOut);
                    payload = out.toString();
                }
            } else {
                // Fallback: try XML index
                org.w3c.dom.Element xmlRoot = xmlSubmodelById.get(id);
                if (xmlRoot != null) {
                    String idShort = firstTextByLocalName(xmlRoot, "idShort");
                    com.fasterxml.jackson.databind.node.ObjectNode out = objectMapper.createObjectNode();
                    out.put("modelType", "Submodel");
                    if (id != null) out.put("id", id);
                    if (idShort != null) out.put("idShort", idShort);
                    out.putArray("submodelElements");
                    payload = out.toString();
                } else {
                    // Best-effort: create minimal submodel from selected id
                    com.fasterxml.jackson.databind.node.ObjectNode out = objectMapper.createObjectNode();
                    out.put("modelType", "Submodel");
                    out.put("id", id);
                    out.put("idShort", deriveIdShortFromId(id));
                    out.putArray("submodelElements");
                    payload = out.toString();
                }
            }

            try {
                String smIdB64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(id.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                var check = traversalClient.getSubmodel(ts.apiUrl, smIdB64, headersRead).await().indefinitely();
                if (check.statusCode() >= 200 && check.statusCode() < 300) {
                    if (tryAddSubmodelRef(ts, id, headersWrite, headersRead)) attached++;
                    continue;
                }
            } catch (Exception ignore) { }

            var createResp = traversalClient.createSubmodel(ts.apiUrl, payload, headersWrite).await().indefinitely();
            if (createResp.statusCode() >= 200 && createResp.statusCode() < 300) {
                String createdId = id;
                try {
                    String b = createResp.bodyAsString();
                    if (b != null && !b.isBlank() && b.trim().startsWith("{")) {
                        JsonNode created = objectMapper.readTree(b);
                        String cid = textOrNull(created, "id");
                        if (cid != null && !cid.isBlank()) createdId = cid;
                    }
                } catch (Exception ignore) { }
                if (tryAddSubmodelRef(ts, createdId, headersWrite, headersRead)) attached++;
            } else {
                Log.warnf("attachSelected(target): failed to create submodel id=%s upstream: %d %s", id, createResp.statusCode(), createResp.statusMessage());
            }
        }
        return attached;
    }

    private boolean tryAddSubmodelRef(SourceSystem ss, String submodelId, java.util.Map<String,String> headersWrite, java.util.Map<String,String> headersRead) {
        try {
            // Check if ref already present
            var refsResp = traversalClient.listSubmodelReferences(ss.apiUrl, ss.aasId, headersRead).await().indefinitely();
            if (refsResp.statusCode() >= 200 && refsResp.statusCode() < 300) {
                String body = refsResp.bodyAsString();
                io.vertx.core.json.JsonArray arr;
                if (body != null && body.trim().startsWith("{")) {
                    io.vertx.core.json.JsonObject obj = new io.vertx.core.json.JsonObject(body);
                    arr = obj.getJsonArray("result", new io.vertx.core.json.JsonArray());
                } else {
                    arr = new io.vertx.core.json.JsonArray(body);
                }
                for (int i = 0; i < arr.size(); i++) {
                    var ref = arr.getJsonObject(i);
                    var keys = ref.getJsonArray("keys");
                    if (keys != null && !keys.isEmpty()) {
                        var k0 = keys.getJsonObject(0);
                        if ("Submodel".equalsIgnoreCase(k0.getString("type")) && submodelId.equals(k0.getString("value"))) {
                            return true; // already present
                        }
                    }
                }
            }
            var add = traversalClient.addSubmodelReferenceToShell(ss.apiUrl, ss.aasId, submodelId, headersWrite).await().indefinitely();
            if (add.statusCode() >= 200 && add.statusCode() < 300) {
                // Verify presence
                try {
                    var refsResp2 = traversalClient.listSubmodelReferences(ss.apiUrl, ss.aasId, headersRead).await().indefinitely();
                    if (refsResp2.statusCode() >= 200 && refsResp2.statusCode() < 300) {
                        String body2 = refsResp2.bodyAsString();
                        io.vertx.core.json.JsonArray arr2;
                        if (body2 != null && body2.trim().startsWith("{")) {
                            io.vertx.core.json.JsonObject obj2 = new io.vertx.core.json.JsonObject(body2);
                            arr2 = obj2.getJsonArray("result", new io.vertx.core.json.JsonArray());
                        } else {
                            arr2 = new io.vertx.core.json.JsonArray(body2);
                        }
                        for (int i = 0; i < arr2.size(); i++) {
                            var ref = arr2.getJsonObject(i);
                            var keys = ref.getJsonArray("keys");
                            if (keys != null && !keys.isEmpty()) {
                                var k0 = keys.getJsonObject(0);
                                if ("Submodel".equalsIgnoreCase(k0.getString("type")) && submodelId.equals(k0.getString("value"))) {
                                    return true;
                                }
                            }
                        }
                    }
                } catch (Exception ignore) { }
            } else {
                Log.warnf("Add submodel-ref failed id=%s: %d %s", submodelId, add.statusCode(), add.statusMessage());
            }
        } catch (Exception e) {
            Log.warnf(e, "Failed to add submodel-ref for id=%s to shell=%s", submodelId, ss.aasId);
        }
        return false;
    }

    private boolean tryAddSubmodelRef(TargetSystem ts, String submodelId, java.util.Map<String,String> headersWrite, java.util.Map<String,String> headersRead) {
        try {
            var refsResp = traversalClient.listSubmodelReferences(ts.apiUrl, ts.aasId, headersRead).await().indefinitely();
            if (refsResp.statusCode() >= 200 && refsResp.statusCode() < 300) {
                String body = refsResp.bodyAsString();
                io.vertx.core.json.JsonArray arr;
                if (body != null && body.trim().startsWith("{")) {
                    io.vertx.core.json.JsonObject obj = new io.vertx.core.json.JsonObject(body);
                    arr = obj.getJsonArray("result", new io.vertx.core.json.JsonArray());
                } else {
                    arr = new io.vertx.core.json.JsonArray(body);
                }
                for (int i = 0; i < arr.size(); i++) {
                    var ref = arr.getJsonObject(i);
                    var keys = ref.getJsonArray("keys");
                    if (keys != null && !keys.isEmpty()) {
                        var k0 = keys.getJsonObject(0);
                        if ("Submodel".equalsIgnoreCase(k0.getString("type")) && submodelId.equals(k0.getString("value"))) {
                            return true;
                        }
                    }
                }
            }
            var add = traversalClient.addSubmodelReferenceToShell(ts.apiUrl, ts.aasId, submodelId, headersWrite).await().indefinitely();
            if (add.statusCode() >= 200 && add.statusCode() < 300) {
                // Verify presence like Source implementation
                try {
                    var refsResp2 = traversalClient.listSubmodelReferences(ts.apiUrl, ts.aasId, headersRead).await().indefinitely();
                    if (refsResp2.statusCode() >= 200 && refsResp2.statusCode() < 300) {
                        String body2 = refsResp2.bodyAsString();
                        io.vertx.core.json.JsonArray arr2;
                        if (body2 != null && body2.trim().startsWith("{")) {
                            io.vertx.core.json.JsonObject obj2 = new io.vertx.core.json.JsonObject(body2);
                            arr2 = obj2.getJsonArray("result", new io.vertx.core.json.JsonArray());
                        } else {
                            arr2 = new io.vertx.core.json.JsonArray(body2);
                        }
                        for (int i = 0; i < arr2.size(); i++) {
                            var ref = arr2.getJsonObject(i);
                            var keys = ref.getJsonArray("keys");
                            if (keys != null && !keys.isEmpty()) {
                                var k0 = keys.getJsonObject(0);
                                if ("Submodel".equalsIgnoreCase(k0.getString("type")) && submodelId.equals(k0.getString("value"))) {
                                    return true;
                                }
                            }
                        }
                    }
                } catch (Exception ignore) { }
            } else {
                Log.warnf("Add submodel-ref failed (target) id=%s: %d %s", submodelId, add.statusCode(), add.statusMessage());
            }
        } catch (Exception e) {
            Log.warnf(e, "Failed to add submodel-ref for id=%s to target shell=%s", submodelId, ts.aasId);
        }
        return false;
    }

    private String buildNormalizedSubmodelPayload(JsonNode smNode) {
        // Extract minimal required fields and normalize to BaSyx-friendly payload
        String id = textOrNull(smNode, "id");
        if (id == null && smNode.has("identification")) {
            id = textOrNull(smNode.get("identification"), "id");
        }
        String idShort = textOrNull(smNode, "idShort");
        com.fasterxml.jackson.databind.node.ObjectNode out = objectMapper.createObjectNode();
        if (id != null) out.put("id", id);
        if (idShort != null) out.put("idShort", idShort);
        out.put("modelType", "Submodel");
        // Prefer existing submodelElements if present
        JsonNode elements = smNode.get("submodelElements");
        if (elements != null && elements.isArray()) {
            out.set("submodelElements", elements);
        } else {
            out.putArray("submodelElements");
        }
        return out.toString();
    }

    private void ingestElement(AasSubmodelLite submodel, String parentPath, JsonNode n, java.util.Set<String> seen) {
        String idShort = textOrNull(n, "idShort");
        if (idShort == null) return;
        String modelType = textOrNull(n, "modelType");
        // Heuristic: detect MultiLanguageProperty when value is array of {language,text}
        if (modelType == null || modelType.isBlank()) {
            JsonNode val = n.get("value");
            if (val != null && val.isArray() && val.size() > 0) {
                JsonNode first = val.get(0);
                if (first != null && first.has("language") && first.has("text")) {
                    modelType = "MultiLanguageProperty";
                }
            }
        }
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

        // Verify submodel exists upstream to avoid persisting stale refs
        try {
            String smIdB64 = toBase64Url(id);
            var headers = headerBuilder.buildMergedHeaders(ss, HttpHeaderBuilder.Mode.READ);
            HttpResponse<Buffer> smResp = traversalClient.getSubmodel(ss.apiUrl, smIdB64, headers).await().indefinitely();
            if (smResp.statusCode() < 200 || smResp.statusCode() >= 300) {
                Log.infof("Skip persisting submodel %s due to upstream status %d", id, smResp.statusCode());
                return;
            }
            // Prefer authoritative payload from upstream
            n = objectMapper.readTree(smResp.bodyAsString());
        } catch (Exception e) {
            Log.warnf(e, "Failed to verify submodel %s existence; skipping", id);
            return;
        }

        String idShort = textOrNull(n, "idShort");
        String semanticId = textOrNull(n, "semanticId");
        String kind = textOrNull(n, "kind");

        // enrichment already done via authoritative payload above

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

    private String deriveIdShortFromId(String id) {
        if (id == null) return null;
        String s = id;
        String[] parts = s.split("/");
        if (parts.length >= 3) {
            String last = parts[parts.length - 1];
            String prev = parts[parts.length - 2];
            String prev2 = parts[parts.length - 3];
            boolean lastNum = last.matches("\\d+");
            boolean prevNum = prev.matches("\\d+");
            if (lastNum && prevNum) {
                return prev2;
            }
            if (lastNum && !prevNum) {
                return prev;
            }
        }
        int hash = s.lastIndexOf('#');
        int slash = s.lastIndexOf('/');
        int idx = Math.max(hash, slash);
        if (idx >= 0 && idx < s.length() - 1) return s.substring(idx + 1);
        return s;
    }

    private boolean importSubmodelFromJson(SourceSystem ss, JsonNode smNode) {
        if (smNode == null || smNode.isNull()) return false;
        // Expect either a standalone submodel object or an entry within a container
        JsonNode node = smNode;
        // Some containers wrap submodel under 'submodel' key
        if (node.has("submodel")) node = node.get("submodel");

        String id = textOrNull(node, "id");
        if (id == null && node.has("identification")) {
            id = textOrNull(node.get("identification"), "id");
        }
        if (id == null || id.isBlank()) return false;

        var existingSm = AasSubmodelLite.<AasSubmodelLite>find("sourceSystem.id = ?1 and submodelId = ?2", ss.id, id).firstResult();
        if (existingSm != null) {
            return false; // already present; don't duplicate
        }

        String idShort = textOrNull(node, "idShort");
        String kind = textOrNull(node, "kind");
        String semanticId = null;
        JsonNode semNode = node.get("semanticId");
        if (semNode != null) {
            // semanticId may be object or simple value
            if (semNode.isTextual()) {
                semanticId = semNode.asText();
            } else if (semNode.has("keys") && semNode.get("keys").isArray() && semNode.get("keys").size() > 0) {
                JsonNode k0 = semNode.get("keys").get(0);
                semanticId = textOrNull(k0, "value");
            }
        }

        AasSubmodelLite sm = new AasSubmodelLite();
        sm.sourceSystem = ss;
        sm.submodelId = id;
        sm.submodelIdShort = idShort;
        sm.semanticId = semanticId;
        sm.kind = kind;
        sm.persist();

        JsonNode elementsNode = node.get("submodelElements");
        if (elementsNode != null && elementsNode.isArray()) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (JsonNode el : elementsNode) {
                ingestElement(sm, null, el, seen);
            }
        }
        return true;
    }

    private void persistElementLite(AasSubmodelLite submodel, String parentPath, JsonNode n, Set<String> seen) {
        String idShort = textOrNull(n, "idShort");
        if (idShort == null) return;
        String modelType = textOrNull(n, "modelType");
        if (modelType == null || modelType.isBlank()) {
            JsonNode val = n.get("value");
            if (val != null && val.isArray() && val.size() > 0) {
                JsonNode first = val.get(0);
                if (first != null && first.has("language") && first.has("text")) {
                    modelType = "MultiLanguageProperty";
                }
            }
        }
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
        var submodel = AasSubmodelLite.<AasSubmodelLite>find("sourceSystem.id = ?1 and submodelId = ?2", sourceSystemId, normalizeSubmodelId(submodelId)).firstResult();
        if (submodel == null) return;
        AasElementLite.delete("submodelLite.id", submodel.id);
        submodel.delete();
    }

    @Transactional
    public void applyElementDelete(Long sourceSystemId, String submodelId, String idShortPath) {
        var submodel = AasSubmodelLite.<AasSubmodelLite>find("sourceSystem.id = ?1 and submodelId = ?2", sourceSystemId, normalizeSubmodelId(submodelId)).firstResult();
        if (submodel == null) return;
        AasElementLite.delete("submodelLite.id = ?1 and (idShortPath = ?2 or idShortPath like ?3)", submodel.id, idShortPath, idShortPath + "/%");
    }

    private String normalizeSubmodelId(String smId) {
        if (smId == null) return null;
        try {
            byte[] decoded = java.util.Base64.getUrlDecoder().decode(smId);
            String plain = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            return plain;
        } catch (IllegalArgumentException e) {
            return smId;
        }
    }
}


