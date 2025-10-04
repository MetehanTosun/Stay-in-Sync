package de.unistuttgart.stayinsync.core.configuration.rest.aas.target;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient;
import de.unistuttgart.stayinsync.core.configuration.service.aas.TargetSystemAasService;
import de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder;
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
 * AAS Submodel Management Controller for Target Systems.
 * Handles CRUD operations for AAS submodels.
 */
@Path("/api/config/target-system/{targetSystemId}/aas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
public class TargetAasSubmodelController {

    @Inject
    TargetSystemAasService aasService;

    @Inject
    AasTraversalClient traversal;

    @Inject
    HttpHeaderBuilder headerBuilder;

    @POST
    @Path("/submodels")
    @Operation(summary = "Create submodel", description = "Creates a new submodel in the target AAS")
    @APIResponses(value = {
        @APIResponse(responseCode = "201", description = "Submodel created successfully"),
        @APIResponse(responseCode = "400", description = "Invalid request"),
        @APIResponse(responseCode = "404", description = "Target system not found"),
        @APIResponse(responseCode = "500", description = "Failed to create submodel")
    })
    public Response createSubmodel(@PathParam("targetSystemId") Long targetSystemId, 
                                  @RequestBody(description = "Submodel JSON", content = @Content(schema = @Schema(implementation = String.class))) String body) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.WRITE_JSON);
        var resp = traversal.createSubmodel(ts.apiUrl, body, headers).await().indefinitely();
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return Response.status(Response.Status.CREATED).entity(resp.bodyAsString()).build();
        }
        return aasService.mapHttpError(resp.statusCode(), resp.statusMessage(), resp.bodyAsString());
    }

    @PUT
    @Path("/submodels/{smId}")
    @Operation(summary = "Update submodel", description = "Updates an existing submodel in the target AAS")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Submodel updated successfully"),
        @APIResponse(responseCode = "400", description = "Invalid request"),
        @APIResponse(responseCode = "404", description = "Target system or submodel not found"),
        @APIResponse(responseCode = "500", description = "Failed to update submodel")
    })
    public Response putSubmodel(@PathParam("targetSystemId") Long targetSystemId,
                                @PathParam("smId") String smId,
                                @RequestBody(description = "Submodel JSON", content = @Content(schema = @Schema(implementation = String.class))) String body) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.WRITE_JSON);
        var resp = traversal.putSubmodel(ts.apiUrl, smId, body, headers).await().indefinitely();
        return Response.status(resp.statusCode()).entity(resp.bodyAsString()).build();
    }

    @DELETE
    @Path("/submodels/{smId}")
    @Operation(summary = "Delete submodel", description = "Deletes a submodel from the target AAS")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Submodel deleted successfully"),
        @APIResponse(responseCode = "404", description = "Target system or submodel not found"),
        @APIResponse(responseCode = "500", description = "Failed to delete submodel")
    })
    public Response deleteSubmodel(@PathParam("targetSystemId") Long targetSystemId,
                                   @PathParam("smId") String smId) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.WRITE_JSON);
        var resp = traversal.deleteSubmodel(ts.apiUrl, smId, headers).await().indefinitely();
        return Response.status(resp.statusCode()).entity(resp.bodyAsString()).build();
    }
}
