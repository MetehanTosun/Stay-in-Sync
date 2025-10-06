package de.unistuttgart.stayinsync.syncnode.syncjob.assets;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/sync-node/asset-cache")
@Produces(MediaType.APPLICATION_JSON)
public class AssetCacheResource {

    @Inject
    CheckResponseCacheService cacheService;

    @GET
    @Path("/{transformationId}")
    public Response getResponsesForTransformation(@PathParam("transformationId") Long transformationId) {
        return cacheService.getResponsesByTransformationId(transformationId)
                .map(data -> Response.ok(data).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity("No cached responses found for transformation ID: " + transformationId)
                        .build());
    }
}