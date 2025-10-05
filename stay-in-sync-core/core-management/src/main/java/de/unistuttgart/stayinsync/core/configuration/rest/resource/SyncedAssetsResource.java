package de.unistuttgart.stayinsync.core.configuration.rest.resource;

import de.unistuttgart.stayinsync.core.configuration.rest.Examples;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SyncJobDTO;
import de.unistuttgart.stayinsync.core.configuration.service.SyncedAssetsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/api/config/sync-job")
@Produces(APPLICATION_JSON)
public class SyncedAssetsResource {

    @Inject
    SyncedAssetsService syncedAssetsService;

    @GET
    @Path("/{id}")
    @Operation(summary = "Returns synced-assets for a transformation id")
    @APIResponse(
            responseCode = "200",
            description = "Gets synced-assets for transformation id",
            content = @Content(
                    mediaType = APPLICATION_JSON
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "No Synced-assets found for transformation id"
    )
    public Response getSyncedAssets(@Parameter(name = "id", required = true) @PathParam("id") Long id) {

        syncedAssetsService.retrieveSyncedAssets(id);

    }
}
