package de.unistuttgart.stayinsync.syncnode.SnapshotManagement;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/monitoring/snapshots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SnapshotResource {

    @Inject
    SnapshotStore store;
    @Inject
    ObjectMapper om;

    /**
     * Internal hook (optional): accept a TransformationResult and turn it into a
     * Snapshot.
     * Useful for manual testing without wiring the executor yet.
     */
    @POST
    @Path("/from-result")
    public Response createFromResult(TransformationResult tr) {
        if (tr == null)
            return Response.status(Response.Status.BAD_REQUEST).entity("empty body").build();
        SnapshotDTO snapshot = SnapshotFactory.fromTransformationResult(tr, om);
        store.put(snapshot);
        return Response.status(Response.Status.CREATED).entity(snapshot).build();
    }

    @GET
    @Path("/{snapshotId}")
    public Response byId(@PathParam("snapshotId") String id) {
        return store.getBySnapshotId(id)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @GET
    @Path("/latest")
    public Response latest(@QueryParam("transformationId") Long transformationId) {
        if (transformationId == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("transformationId is required").build();
        }
        return store.getLatestByTransformationId(transformationId)
                .map(Response::ok)
                .orElse(Response.status(Response.Status.NOT_FOUND))
                .build();
    }

    @GET
    @Path("/list")
    public Response list(@QueryParam("transformationId") Long transformationId,
            @QueryParam("limit") @DefaultValue("5") int limit) {
        if (transformationId == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("transformationId is required").build();
        }
        return Response.ok(store.listByTransformationId(transformationId, Math.max(1, limit))).build();
    }
}