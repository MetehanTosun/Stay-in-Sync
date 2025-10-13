package de.unistuttgart.stayinsync.monitoring.core.configuration.clients;

import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/monitoring/snapshots") // Pfad zur SnapshotResource im core-sync-node
@RegisterRestClient(configKey = "snapshot-api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface SnapshotClient {

    @GET
    @Path("/{snapshotId}")
    SnapshotDTO byId(@PathParam("snapshotId") String id);

    // optional: latest by jobId
    @GET
    @Path("/latest")
    SnapshotDTO latest(@QueryParam("jobId") String jobId);
}