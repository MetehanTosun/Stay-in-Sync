package de.unistuttgart.stayinsync.core.configuration.rest.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.service.aas.AasTraversalClient;
import de.unistuttgart.stayinsync.core.configuration.service.aas.SourceSystemAasService;
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
 * AAS Value Management Controller for Source Systems.
 * Handles value operations for AAS elements within submodels.
 */
@Path("/api/config/source-system/{sourceSystemId}/aas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
public class AasValueController {

    @Inject
    SourceSystemAasService aasService;

    @Inject
    AasTraversalClient traversal;

    @Inject
    HttpHeaderBuilder headerBuilder;

    @GET
    @Path("/submodels/{smId}/elements/{path:.+}/value")
    @Operation(summary = "Get element value", description = "Retrieves the value of an element")
    @APIResponses(value = {
        @APIResponse(responseCode = "200", description = "Element value retrieved successfully"),
        @APIResponse(responseCode = "404", description = "Source system, submodel or element not found"),
        @APIResponse(responseCode = "500", description = "Failed to retrieve element value")
    })
    public Uni<Response> getElementValue(@PathParam("sourceSystemId") Long sourceSystemId,
                                        @PathParam("smId") String smId,
                                        @PathParam("path") String path) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        var headers = headerBuilder.buildMergedHeaders(ss, HttpHeaderBuilder.Mode.READ);
        Log.infof("Get element value LIVE: apiUrl=%s smId=%s path=%s", ss.apiUrl, smId, path);
        return traversal.getElement(ss.apiUrl, smId, path + "/value", headers).map(resp -> {
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
        @APIResponse(responseCode = "404", description = "Source system, submodel or element not found"),
        @APIResponse(responseCode = "500", description = "Failed to update element value")
    })
    public Response patchElementValue(@PathParam("sourceSystemId") Long sourceSystemId,
                                      @PathParam("smId") String smId,
                                      @PathParam("path") String path,
                                      @RequestBody(description = "Element value JSON", content = @Content(schema = @Schema(implementation = String.class))) String body) {
        SourceSystem ss = SourceSystem.<SourceSystem>findByIdOptional(sourceSystemId).orElse(null);
        ss = aasService.validateAasSource(ss);
        var headers = headerBuilder.buildMergedHeaders(ss, HttpHeaderBuilder.Mode.WRITE_JSON);
        Log.infof("Patch element value LIVE: apiUrl=%s smId=%s path=%s", ss.apiUrl, smId, path);
        Log.debugf("WRITE headers: %s body=%s", headers, body);
        var resp = traversal.patchElementValue(ss.apiUrl, smId, path, body, headers).await().indefinitely();
        int sc = resp.statusCode();
        Log.infof("Patch element upstream status=%d msg=%s body=%s", sc, resp.statusMessage(), safeBody(resp));
        if (sc >= 200 && sc < 300) {
            return Response.noContent().build();
        }
        return aasService.mapHttpError(sc, resp.statusMessage(), resp.bodyAsString());
    }

    private String safeBody(HttpResponse<Buffer> resp) {
        try {
            String b = resp.bodyAsString();
            if (b == null) return null;
            return b.length() > 500 ? b.substring(0, 500) + "..." : b;
        } catch (Exception e) {
            return null;
        }
    }
}
