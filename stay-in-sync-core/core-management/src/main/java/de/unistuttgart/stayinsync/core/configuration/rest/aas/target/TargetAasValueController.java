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
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

/**
 * AAS Value Management Controller for Target Systems.
 * Handles value operations for AAS elements within submodels.
 */
@Path("/api/config/target-system/{targetSystemId}/aas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
public class TargetAasValueController {

    @Inject
    TargetSystemAasService aasService;

    @Inject
    AasTraversalClient traversal;

    @Inject
    HttpHeaderBuilder headerBuilder;

    @GET
    @Path("/submodels/{smId}/elements/{path:.+}/value")
    @Operation(summary = "Get element value", description = "Retrieves the value of an element")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Element value retrieved successfully"),
        @APIResponse(responseCode = "404", description = "Target system, submodel or element not found"),
        @APIResponse(responseCode = "500", description = "Failed to retrieve element value")
    })
    public Uni<Response> getElementValue(@PathParam("targetSystemId") Long targetSystemId,
                                        @PathParam("smId") String smId,
                                        @PathParam("path") String path) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.READ);
        return traversal.getElement(ts.apiUrl, smId, path + "/value", headers).map(resp -> {
            int sc = resp.statusCode();
            if (sc >= 200 && sc < 300) {
                return Response.ok(resp.bodyAsString()).build();
            }
            return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
        });
    }

    @PATCH
    @Path("/submodels/{smId}/elements/{path:.+}/value")
    @Operation(summary = "Update element value", description = "Updates the value of an element")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "Element value updated successfully"),
        @APIResponse(responseCode = "400", description = "Invalid request"),
        @APIResponse(responseCode = "404", description = "Target system, submodel or element not found"),
        @APIResponse(responseCode = "500", description = "Failed to update element value")
    })
    public Response patchElementValue(@PathParam("targetSystemId") Long targetSystemId,
                                      @PathParam("smId") String smId,
                                      @PathParam("path") String path,
                                      @RequestBody(description = "Element value JSON", content = @Content(schema = @Schema(implementation = String.class))) String body) {
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.WRITE_JSON);
        var resp = traversal.patchElementValue(ts.apiUrl, smId, path, body, headers).await().indefinitely();
        return Response.status(resp.statusCode()).entity(resp.bodyAsString()).build();
    }
}
