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
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(fileBytes))) {
            boolean hasPayload = false;
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null) continue;
                hasPayload = true; // minimal validation that it is a zip with entries
                // TODO: parse AASX relationships and submodel JSON/XML parts as needed
                // For initial implementation, we accept the upload and trigger a snapshot refresh from LIVE
            }
            if (!hasPayload) {
                throw new InvalidAasxException("Not a valid AASX: archive has no entries");
            }
        } catch (java.io.IOException e) {
            throw new InvalidAasxException("Failed to read AASX: " + e.getMessage());
        }
        // For v1: trigger a refresh using existing LIVE-based snapshot builder as a placeholder
        // Future: replace with actual parsed SubmodelLite/ElementLite persistence from the AASX content
        refreshSnapshot(sourceSystemId);
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


