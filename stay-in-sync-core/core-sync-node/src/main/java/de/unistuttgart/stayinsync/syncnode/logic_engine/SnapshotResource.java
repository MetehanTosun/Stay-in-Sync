package de.unistuttgart.stayinsync.syncnode.logic_engine;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/snapshot")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SnapshotResource {

    @Inject
    SnapshotCacheService cacheService;

    @GET
    @Path("/{id}")
    public Response getSnapshot(@PathParam("id") Long id) {
        return cacheService.getSnapshot(id)
                .map(snapshotJson -> Response.ok(snapshotJson).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path("/{id}")
    public Response saveSnapshot(@PathParam("id") Long id, String snapshotJson) {
        if (snapshotJson == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        cacheService.saveSnapshot(id, snapshotJson);
        return Response.ok().build();
    }
}
