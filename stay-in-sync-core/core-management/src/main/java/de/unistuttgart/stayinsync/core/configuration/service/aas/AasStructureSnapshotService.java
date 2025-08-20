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
import jakarta.inject.Inject;

import java.util.Map;

@ApplicationScoped
public class AasStructureSnapshotService {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    AasTraversalClient traversalClient;

    @Inject
    HttpHeaderBuilder headerBuilder;

    public void buildInitialSnapshot(Long sourceSystemId) {
        refreshSnapshot(sourceSystemId);
    }

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
        } catch (Exception e) {
            Log.errorf(e, "Failed to refresh AAS snapshot for sourceSystemId=%d", sourceSystemId);
        }
    }

    private void persistSubmodelLite(SourceSystem ss, JsonNode n) {
        String id = textOrNull(n, "id");
        if (id == null && n.has("identification")) {
            id = textOrNull(n.get("identification"), "id");
        }
        if (id == null) return;

        AasSubmodelLite sm = new AasSubmodelLite();
        sm.sourceSystem = ss;
        sm.submodelId = id;
        sm.submodelIdShort = textOrNull(n, "idShort");
        sm.semanticId = textOrNull(n, "semanticId");
        sm.kind = textOrNull(n, "kind");
        sm.persist();
    }

    private String textOrNull(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode f = node.get(field);
        return f != null && !f.isNull() ? f.asText() : null;
    }
}


