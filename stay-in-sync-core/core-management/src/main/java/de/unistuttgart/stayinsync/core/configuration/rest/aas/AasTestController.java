package de.unistuttgart.stayinsync.core.configuration.rest.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.AasTestResultDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas.SubmodelSummaryDTO;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient;
import de.unistuttgart.stayinsync.core.configuration.service.aas.SourceSystemAasService;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.core.buffer.Buffer;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import java.util.ArrayList;
import java.util.List;

/**
 * AAS Test and Discovery Controller for Source Systems.
 * Handles AAS connectivity testing and submodel discovery.
 */
@Path("/api/config/source-system/{sourceSystemId}/aas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
public class AasTestController {

    @Inject
    SourceSystemAasService aasService;

    @Inject
    AasTraversalClient traversal;

    @Inject
    de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder headerBuilder;

    @POST
    @Path("/test")
    @Operation(summary = "Test AAS connectivity", description = "Tests the connectivity to the AAS server")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "AAS test successful"),
        @APIResponse(responseCode = "400", description = "Invalid request"),
        @APIResponse(responseCode = "404", description = "Source system not found"),
        @APIResponse(responseCode = "500", description = "AAS test failed")
    })
    public Uni<Response> test(@PathParam("sourceSystemId") Long sourceSystemId) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        Log.infof("AAS test: sourceSystemId=%d apiUrl=%s aasId=%s", sourceSystemId, ss.apiUrl, ss.aasId);
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
        Log.debugf("AAS test headers: %s", headers);
        Uni<HttpResponse<Buffer>> uni = traversal.getShell(ss.apiUrl, ss.aasId, headers);
        return uni.map(resp -> {
                    int sc = resp.statusCode();
                    Log.infof("AAS test upstream status=%d msg=%s body=%s", sc, resp.statusMessage(), safeBody(resp));
                    if (sc >= 200 && sc < 300) {
                        return Response.ok(new AasTestResultDTO("shell", "asset")).build();
                    }
                    Log.warnf("AAS test failed: %d %s", sc, resp.statusMessage());
                    return Response.status(sc).entity(resp.bodyAsString()).build();
                });
    }

    @GET
    @Path("/submodels")
    @Operation(summary = "List submodels", description = "Lists all submodels from the AAS")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Submodels retrieved successfully"),
        @APIResponse(responseCode = "400", description = "Invalid request"),
        @APIResponse(responseCode = "404", description = "Source system not found"),
        @APIResponse(responseCode = "500", description = "Failed to retrieve submodels")
    })
    public Uni<Response> listSubmodels(@PathParam("sourceSystemId") Long sourceSystemId,
                                  @QueryParam("source") @DefaultValue("SNAPSHOT") String source) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        if ("LIVE".equalsIgnoreCase(source)) {
            var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.READ);
            final String apiUrl = ss.apiUrl;
            final String aasId = ss.aasId;
            final java.util.Map<String,String> headersLive = headers;
            Log.infof("List submodels LIVE: apiUrl=%s aasId=%s", apiUrl, aasId);
            Log.debugf("LIVE headers: %s", headersLive);
            return traversal.listSubmodels(apiUrl, aasId, headersLive).map(resp -> {
                int sc = resp.statusCode();
                Log.infof("List submodels upstream status=%d msg=%s", sc, resp.statusMessage());
                if (sc >= 200 && sc < 300) {
                    String body = resp.bodyAsString();
                    try {
                        // Filter out refs that point to non-existing submodels (404), to avoid stale entries
                        io.vertx.core.json.JsonArray refs;
                        boolean wrapped = false;
                        if (body != null && body.trim().startsWith("{")) {
                            io.vertx.core.json.JsonObject obj = new io.vertx.core.json.JsonObject(body);
                            refs = obj.getJsonArray("result", new io.vertx.core.json.JsonArray());
                            wrapped = true;
                        } else {
                            refs = new io.vertx.core.json.JsonArray(body);
                        }
                        io.vertx.core.json.JsonArray filtered = new io.vertx.core.json.JsonArray();
                        for (int i = 0; i < refs.size(); i++) {
                            io.vertx.core.json.JsonObject ref = refs.getJsonObject(i);
                            String submodelId = ref.getString("keys", "");
                            if (submodelId != null && !submodelId.isEmpty()) {
                                // Test if this submodel actually exists by trying to get it
                                try {
                                    var testResp = traversal.getSubmodel(apiUrl, submodelId, headersLive).await().indefinitely();
                                    if (testResp.statusCode() >= 200 && testResp.statusCode() < 300) {
                                        filtered.add(ref);
                                    } else {
                                        Log.warnf("Filtering out stale submodel ref: %s (status: %d)", submodelId, testResp.statusCode());
                                    }
                                } catch (Exception e) {
                                    Log.warnf("Filtering out stale submodel ref: %s (error: %s)", submodelId, e.getMessage());
                                }
                            }
                        }
                        List<SubmodelSummaryDTO> result = new ArrayList<>();
                        for (int i = 0; i < filtered.size(); i++) {
                            io.vertx.core.json.JsonObject ref = filtered.getJsonObject(i);
                            String submodelId = ref.getString("keys", "");
                            String idShort = ref.getString("idShort", "");
                            result.add(new SubmodelSummaryDTO(submodelId, idShort, null, null, null));
                        }
                        return Response.ok(result).build();
                    } catch (Exception e) {
                        Log.errorf("Failed to parse submodel refs: %s", e.getMessage());
                        return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
                    }
                }
                return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
            });
        } else {
            // SNAPSHOT source
            Log.infof("List submodels SNAPSHOT: sourceSystemId=%d", sourceSystemId);
            List<de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasSubmodelLite> submodels = 
                de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasSubmodelLite.list("sourceSystem.id", sourceSystemId);
            List<SubmodelSummaryDTO> result = new ArrayList<>();
            for (de.unistuttgart.stayinsync.core.configuration.domain.entities.aas.AasSubmodelLite sm : submodels) {
                result.add(new SubmodelSummaryDTO(sm.submodelId, sm.submodelIdShort, sm.semanticId, sm.kind, sm.id));
            }
            return Uni.createFrom().item(Response.ok(result).build());
        }
    }

    private static String safeBody(HttpResponse<Buffer> resp) {
        try {
            String body = resp.bodyAsString();
            return body != null && body.length() > 200 ? body.substring(0, 200) + "..." : body;
        } catch (Exception e) {
            return "<error reading body>";
        }
    }
}
