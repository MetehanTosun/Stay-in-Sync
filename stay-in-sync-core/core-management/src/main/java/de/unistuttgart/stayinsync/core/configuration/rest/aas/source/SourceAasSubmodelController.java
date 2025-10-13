package de.unistuttgart.stayinsync.core.configuration.rest.aas.source;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasStructureSnapshotService;
import de.unistuttgart.stayinsync.core.configuration.service.aas.SourceSystemAasService;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

/**
 * AAS Submodel Management Controller for Source Systems.
 * Handles CRUD operations for AAS submodels.
 */
@Path("/api/config/source-system/{sourceSystemId}/aas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
public class SourceAasSubmodelController {

    @Inject
    SourceSystemAasService aasService;

    @Inject
    AasTraversalClient traversal;

    @Inject
    AasStructureSnapshotService snapshotService;

    @Inject
    de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder headerBuilder;

    @POST
    @Path("/submodels")
    @Operation(summary = "Create submodel", description = "Creates a new submodel in the AAS")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Submodel created successfully"),
        @APIResponse(responseCode = "400", description = "Invalid request"),
        @APIResponse(responseCode = "404", description = "Source system not found"),
        @APIResponse(responseCode = "500", description = "Failed to create submodel")
    })
    public Response createSubmodel(@PathParam("sourceSystemId") Long sourceSystemId, 
                                  @RequestBody(description = "Submodel JSON", content = @Content(schema = @Schema(implementation = String.class))) String body) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.WRITE_JSON);
        Log.infof("Create submodel LIVE: apiUrl=%s", ss.apiUrl);
        Log.debugf("WRITE headers: %s body=%s", headers, body);
        var resp = traversal.createSubmodel(ss.apiUrl, body, headers).await().indefinitely();
        int sc = resp.statusCode();
        Log.infof("Create submodel upstream status=%d msg=%s body=%s", sc, resp.statusMessage(), safeBody(resp));
        if (sc >= 200 && sc < 300) {
            try {
                String submodelId = io.vertx.core.json.JsonObject.mapFrom(io.vertx.core.json.jackson.DatabindCodec.mapper().readTree(resp.bodyAsString())).getString("id");
                if (submodelId == null || submodelId.isBlank()) {
                    var reqId = io.vertx.core.json.JsonObject.mapFrom(io.vertx.core.json.jackson.DatabindCodec.mapper().readTree(body)).getString("id");
                    if (reqId != null && !reqId.isBlank()) submodelId = reqId;
                }
                if (submodelId != null && !submodelId.isBlank()) {
                    var refHeaders = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.WRITE_JSON);
                    var refResp = traversal.addSubmodelReferenceToShell(ss.apiUrl, ss.aasId, submodelId, refHeaders).await().indefinitely();
                    Log.infof("Add submodel-ref upstream status=%d msg=%s body=%s", refResp.statusCode(), refResp.statusMessage(), safeBody(refResp));
                } else {
                    Log.warn("Could not resolve submodelId for auto-referencing");
                }
            } catch (Exception e) {
                Log.warn("Auto-reference add failed", e);
            }
            snapshotService.applySubmodelCreate(sourceSystemId, resp.bodyAsString());
            return Response.status(Response.Status.CREATED).entity(resp.bodyAsString()).build();
        }
        aasService.throwHttpError(sc, resp.statusMessage(), resp.bodyAsString());
        return null; // This line will never be reached due to exception
    }

    @PUT
    @Path("/submodels/{smId}")
    @Operation(summary = "Update submodel", description = "Updates an existing submodel in the AAS")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Submodel updated successfully"),
        @APIResponse(responseCode = "400", description = "Invalid request"),
        @APIResponse(responseCode = "404", description = "Source system or submodel not found"),
        @APIResponse(responseCode = "500", description = "Failed to update submodel")
    })
    public Response putSubmodel(@PathParam("sourceSystemId") Long sourceSystemId,
                                @PathParam("smId") String smId,
                                @RequestBody(description = "Submodel JSON", content = @Content(schema = @Schema(implementation = String.class))) String body) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.WRITE_JSON);
        Log.infof("PUT submodel LIVE: apiUrl=%s smId=%s", ss.apiUrl, smId);
        var resp = traversal.putSubmodel(ss.apiUrl, smId, body, headers).await().indefinitely();
        int sc = resp.statusCode();
        Log.infof("PUT submodel upstream status=%d msg=%s body=%s", sc, resp.statusMessage(), safeBody(resp));
        if (sc >= 200 && sc < 300) {
            snapshotService.applySubmodelCreate(sourceSystemId, resp.bodyAsString());
            return Response.ok(resp.bodyAsString()).build();
        }
        aasService.throwHttpError(sc, resp.statusMessage(), resp.bodyAsString());
        return null; // This line will never be reached due to exception
    }

    @DELETE
    @Path("/submodels/{smId}")
    @Operation(summary = "Delete submodel", description = "Deletes a submodel from the AAS")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Submodel deleted successfully"),
        @APIResponse(responseCode = "404", description = "Source system or submodel not found"),
        @APIResponse(responseCode = "500", description = "Failed to delete submodel")
    })
    public Response deleteSubmodel(@PathParam("sourceSystemId") Long sourceSystemId,
                                   @PathParam("smId") String smId) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        var headers = headerBuilder.buildMergedHeaders(ss, de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder.Mode.WRITE_JSON);
        Log.infof("DELETE submodel LIVE: apiUrl=%s smId=%s", ss.apiUrl, smId);
        var resp = traversal.deleteSubmodel(ss.apiUrl, smId, headers).await().indefinitely();
        int sc = resp.statusCode();
        Log.infof("DELETE submodel upstream status=%d msg=%s body=%s", sc, resp.statusMessage(), safeBody(resp));
        if (sc >= 200 && sc < 300) {
            snapshotService.applySubmodelDelete(sourceSystemId, smId);
            return Response.ok().build();
        }
        aasService.throwHttpError(sc, resp.statusMessage(), resp.bodyAsString());
        return null; // This line will never be reached due to exception
    }

    private static String safeBody(io.vertx.mutiny.ext.web.client.HttpResponse<io.vertx.mutiny.core.buffer.Buffer> resp) {
        try {
            String body = resp.bodyAsString();
            return body != null && body.length() > 200 ? body.substring(0, 200) + "..." : body;
        } catch (Exception e) {
            return "<error reading body>";
        }
    }
}
