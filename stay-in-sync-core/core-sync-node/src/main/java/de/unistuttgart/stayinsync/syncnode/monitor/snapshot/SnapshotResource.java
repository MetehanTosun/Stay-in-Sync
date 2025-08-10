package de.unistuttgart.stayinsync.syncnode.monitor.snapshot;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/monitoring/snapshots")
@Produces(MediaType.APPLICATION_JSON)
public class SnapshotResource {

    @Inject
    SnapshotStore store;

    @GET
    @Path("/{snapshotId}")
    public Response getBySnapshotId(@PathParam("snapshotId") String snapshotId) {
        return store.getBySnapshotId(snapshotId)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @GET
    @Path("/latest")
    public Response latestByJob(@QueryParam("jobId") String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("jobId is required").build();
        }
        return store.getLatestByJobId(jobId)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }
}