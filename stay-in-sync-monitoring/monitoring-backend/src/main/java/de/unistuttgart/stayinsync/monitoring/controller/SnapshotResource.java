package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.service.SnapshotService;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/snapshots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SnapshotResource {

    @Inject
    SnapshotService snapshotService;

    @GET
    @Path("/latest")
    public SnapshotDTO getLatestSnapshot(@QueryParam("transformationId") Long transformationId) {
        return snapshotService.getLatestSnapshot(transformationId);
    }

    @GET
    @Path("/list")
    public List<SnapshotDTO> getLastFiveSnapshots(@QueryParam("transformationId") Long transformationId) {
        return snapshotService.getLastFiveSnapshots(transformationId);
    }

    @GET
    @Path("/{id}")
    public SnapshotDTO getById(@PathParam("id") Long id) {
        return snapshotService.getById(id);
    }

}

