package de.unistuttgart.stayinsync.monitoring.core.configuration.clients;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/monitoring/snapshots") // Pfad zur SnapshotResource im core-sync-node
@RegisterRestClient(configKey = "snapshot-api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface SnapshotClient {

    @GET
    @Path("/{snapshotId}")
    SnapshotDTO byId(@PathParam("id") String id);

    // optional: latest by jobId
    @GET
    @Path("/latest")
    SnapshotDTO latest(@QueryParam("jobId") String jobId);
}