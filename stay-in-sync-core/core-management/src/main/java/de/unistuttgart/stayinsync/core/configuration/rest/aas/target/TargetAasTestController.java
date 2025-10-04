package de.unistuttgart.stayinsync.core.configuration.rest.aas.target;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient;
import de.unistuttgart.stayinsync.core.configuration.service.aas.TargetSystemAasService;
import de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder;
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

/**
 * AAS Test and Discovery Controller for Target Systems.
 * Handles AAS connectivity testing and submodel discovery.
 */
@Path("/api/config/target-system/{targetSystemId}/aas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
public class TargetAasTestController {

    @Inject
    TargetSystemAasService aasService;

    @Inject
    AasTraversalClient traversal;

    @Inject
    HttpHeaderBuilder headerBuilder;

    @POST
    @Path("/test")
    @Operation(summary = "Test AAS connectivity", description = "Tests the connectivity to the target AAS server")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "AAS test successful"),
        @APIResponse(responseCode = "400", description = "Invalid request"),
        @APIResponse(responseCode = "404", description = "Target system not found"),
        @APIResponse(responseCode = "500", description = "AAS test failed")
    })
    public Uni<Response> test(@PathParam("targetSystemId") Long targetSystemId) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.READ);
        Log.infof("AAS test (target): id=%d apiUrl=%s aasId=%s", targetSystemId, ts.apiUrl, ts.aasId);
        Uni<HttpResponse<Buffer>> uni = traversal.getShell(ts.apiUrl, ts.aasId, headers);
        return uni.map(resp -> Response.status(resp.statusCode()).entity(resp.bodyAsString()).build());
    }

    @GET
    @Path("/submodels")
    @Operation(summary = "List submodels", description = "Lists all submodels from the target AAS")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Submodels retrieved successfully"),
        @APIResponse(responseCode = "400", description = "Invalid request"),
        @APIResponse(responseCode = "404", description = "Target system not found"),
        @APIResponse(responseCode = "500", description = "Failed to retrieve submodels")
    })
    public Response listSubmodels(@PathParam("targetSystemId") Long targetSystemId,
                                  @QueryParam("source") @DefaultValue("LIVE") String source) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        
        // Target System only supports LIVE (no SNAPSHOT database)
        if ("SNAPSHOT".equalsIgnoreCase(source)) {
            Log.infof("Target listSubmodels: SNAPSHOT requested but not supported, falling back to LIVE");
        }
        
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.READ);
        try {
            var refsResp = traversal.listSubmodels(ts.apiUrl, ts.aasId, headers).await().indefinitely();
            if (refsResp.statusCode() < 200 || refsResp.statusCode() >= 300) {
                return Response.status(refsResp.statusCode()).entity(refsResp.bodyAsString()).build();
            }
            String body = refsResp.bodyAsString();
            Log.infof("Target listSubmodels: upstream status=%d, bodyLen=%d", refsResp.statusCode(), (body == null ? 0 : body.length()));
            io.vertx.core.json.JsonArray refs;
            boolean wrapped = false;
            try {
                if (body != null && body.trim().startsWith("{")) {
                    io.vertx.core.json.JsonObject obj = new io.vertx.core.json.JsonObject(body);
                    refs = obj.getJsonArray("result", new io.vertx.core.json.JsonArray());
                    wrapped = true;
                } else {
                    refs = body != null ? new io.vertx.core.json.JsonArray(body) : new io.vertx.core.json.JsonArray();
                }
            } catch (Exception e) {
                refs = new io.vertx.core.json.JsonArray();
            }
            Log.infof("Target listSubmodels: parsed refs count=%d (wrapped=%s)", refs.size(), wrapped);
            io.vertx.core.json.JsonArray out = new io.vertx.core.json.JsonArray();
            for (int i = 0; i < refs.size(); i++) {
                var ref = refs.getJsonObject(i);
                String submodelId = null;
                try {
                    io.vertx.core.json.JsonArray keys = ref.getJsonArray("keys");
                    if ((keys == null || keys.isEmpty()) && ref.containsKey("value")) {
                        var val = ref.getJsonObject("value");
                        if (val != null) keys = val.getJsonArray("keys");
                    }
                    if (keys != null && !keys.isEmpty()) {
                        var k0 = keys.getJsonObject(0);
                        if (k0 != null && "Submodel".equalsIgnoreCase(k0.getString("type"))) {
                            submodelId = k0.getString("value");
                        } else {
                            submodelId = k0 != null ? k0.getString("value") : null;
                        }
                    }
                } catch (Exception ignore) { }
                Log.infof("Target listSubmodels: ref[%d] -> submodelId=%s", i, submodelId);
                if (submodelId == null) continue;
                try {
                    String normalizedSmId = normalizeSubmodelId(submodelId);
                    String smIdB64 = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(normalizedSmId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    var smResp = traversal.getSubmodel(ts.apiUrl, smIdB64, headers).await().indefinitely();
                    if (smResp != null && smResp.statusCode() >= 200 && smResp.statusCode() < 300) {
                        String smBody = smResp.bodyAsString();
                        var sm = smBody != null && smBody.trim().startsWith("{") ? new io.vertx.core.json.JsonObject(smBody) : new io.vertx.core.json.JsonObject();
                        String id = sm.getString("id", normalizedSmId);
                        String idShort = sm.getString("idShort");
                        if (idShort == null || idShort.isBlank()) {
                            idShort = deriveIdShortFromId(id);
                        }
                        Log.infof("Target listSubmodels: fetched submodel id=%s idShort=%s kind=%s", id, idShort, sm.getString("kind"));
                        io.vertx.core.json.JsonObject item = new io.vertx.core.json.JsonObject()
                                .put("id", id)
                                .put("submodelId", id)
                                .put("idShort", idShort)
                                .put("submodelIdShort", idShort)
                                .put("kind", sm.getString("kind"));
                        out.add(item);
                    } else {
                        Log.infof("Target listSubmodels: filtered stale/invalid submodel-ref value=%s status=%d", normalizedSmId, (smResp != null ? smResp.statusCode() : -1));
                    }
                } catch (Exception ex) {
                    Log.infof("Target listSubmodels: exception resolving submodel %s, skipping: %s", submodelId, ex.getMessage());
                }
            }
            Log.infof("Target listSubmodels: final items=%d", out.size());
            if (wrapped) {
                io.vertx.core.json.JsonObject obj = new io.vertx.core.json.JsonObject().put("result", out);
                return Response.ok(obj.encode()).build();
            }
            return Response.ok(out.encode()).build();
        } catch (Exception e) {
            // Fallback: pass-through refs on error
            var fallback = traversal.listSubmodels(ts.apiUrl, ts.aasId, headers).await().indefinitely();
            return Response.status(fallback.statusCode()).entity(fallback.bodyAsString()).build();
        }
    }

    private String normalizeSubmodelId(String smId) {
        if (smId == null) return null;
        try {
            String s = smId.replace('-', '+').replace('_', '/');
            int pad = (4 - (s.length() % 4)) % 4;
            if (pad > 0) {
                s = s + "====".substring(0, pad);
            }
            byte[] decoded = java.util.Base64.getDecoder().decode(s);
            return new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return smId;
        }
    }

    private String deriveIdShortFromId(String id) {
        if (id == null) return null;
        String s = id;
        int hash = s.lastIndexOf('#');
        int slash = s.lastIndexOf('/');
        // Split by '/'
        String[] parts = s.split("/");
        if (parts.length >= 3) {
            String last = parts[parts.length - 1];
            String prev = parts[parts.length - 2];
            String prev2 = parts[parts.length - 3];
            boolean lastNum = last.matches("\\d+");
            boolean prevNum = prev.matches("\\d+");
            if (lastNum && prevNum) {
                // pattern .../<name>/<version>/<revision>
                return prev2;
            }
            if (lastNum && !prevNum) {
                // pattern .../<name>/<number>
                return prev;
            }
        }
        int idx = Math.max(hash, slash);
        if (idx >= 0 && idx < s.length() - 1) return s.substring(idx + 1);
        return s;
    }
}
