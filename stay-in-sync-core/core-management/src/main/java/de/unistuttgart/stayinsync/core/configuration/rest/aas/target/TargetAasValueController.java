package de.unistuttgart.stayinsync.core.configuration.rest.aas.target;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.TargetSystem;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient;
import de.unistuttgart.stayinsync.core.configuration.service.aas.TargetSystemAasService;
import de.unistuttgart.stayinsync.core.configuration.service.aas.HttpHeaderBuilder;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
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
 * REST controller responsible for managing AAS (Asset Administration Shell) element values
 * within Target Systems. Provides endpoints for retrieving and updating AAS element values
 * through the AasTraversalClient and TargetSystemAasService.
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
    /**
     * Retrieves the current value of a specific AAS element within a submodel.
     * Validates the Target System configuration and performs a GET request to the AAS API.
     * Logs request and response details for traceability.
     *
     * @param targetSystemId The ID of the Target System containing the submodel.
     * @param smId The ID of the submodel containing the element.
     * @param path The hierarchical path to the element whose value should be retrieved.
     * @return A reactive Uni<Response> containing the element value or an error message.
     */
    public Uni<Response> getElementValue(@PathParam("targetSystemId") Long targetSystemId,
                                        @PathParam("smId") String smId,
                                        @PathParam("path") String path) {
        Log.infof("Target getElementValue: targetSystemId=%d smId=%s path=%s", targetSystemId, smId, path);
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.READ);
        return traversal.getElement(ts.apiUrl, smId, path + "/value", headers).map(resp -> {
            int sc = resp.statusCode();
            Log.infof("Target getElementValue upstream status=%d msg=%s", sc, resp.statusMessage());
            if (sc >= 200 && sc < 300) {
                return Response.ok(resp.bodyAsString()).build();
            }
            aasService.throwHttpError(sc, resp.statusMessage(), resp.bodyAsString());
            return null; // This line will never be reached due to exception
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
    /**
     * Updates the value of a specific AAS element within a submodel in the Target System.
     * Validates the Target System configuration and executes a PATCH request to the AAS API.
     * Logs both the request body and upstream response for transparency.
     *
     * @param targetSystemId The ID of the Target System.
     * @param smId The ID of the submodel containing the element.
     * @param path The hierarchical path to the element whose value is to be updated.
     * @param body JSON payload containing the new value for the element.
     * @return HTTP Response indicating the result of the update operation.
     */
    public Response patchElementValue(@PathParam("targetSystemId") Long targetSystemId,
                                      @PathParam("smId") String smId,
                                      @PathParam("path") String path,
                                      @RequestBody(description = "Element value JSON", content = @Content(schema = @Schema(implementation = String.class))) String body) {
        Log.infof("Target patchElementValue: targetSystemId=%d smId=%s path=%s", targetSystemId, smId, path);
        Log.debugf("Target patchElementValue body=%s", body);
        TargetSystem ts = TargetSystem.<TargetSystem>findByIdOptional(targetSystemId).orElse(null);
        ts = aasService.validateAasTarget(ts);
        var headers = headerBuilder.buildMergedHeaders(ts, HttpHeaderBuilder.Mode.WRITE_JSON);
        var resp = traversal.patchElementValue(ts.apiUrl, smId, path, body, headers).await().indefinitely();
        Log.infof("Target patchElementValue upstream status=%d msg=%s", resp.statusCode(), resp.statusMessage());
        return Response.status(resp.statusCode()).entity(resp.bodyAsString()).build();
    }
}
