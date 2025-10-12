package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.service.SnapshotService;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import java.util.List;

@Path("/api/snapshots")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SnapshotResource {

    @Inject
    SnapshotService snapshotService;

    @GET
    @Path("/latest")
    @Operation(
            summary = "Get the latest snapshot for a transformation",
            description = "Returns the most recent snapshot associated with the specified transformation."
    )
    @APIResponse(
            responseCode = "200",
            description = "The latest snapshot for the given transformation",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SnapshotDTO.class)
            )
    )
    public SnapshotDTO getLatestSnapshot(
            @Parameter(description = "The ID of the transformation")
            @QueryParam("transformationId") Long transformationId) {
        return snapshotService.getLatestSnapshot(transformationId);
    }

    @GET
    @Path("/list")
    @Operation(
            summary = "Get the last five snapshots for a transformation",
            description = "Returns up to the last five snapshots associated with the specified transformation, ordered by creation date."
    )
    @APIResponse(
            responseCode = "200",
            description = "List of up to five recent snapshots for the given transformation",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SnapshotDTO.class)
            )
    )
    public List<SnapshotDTO> getLastFiveSnapshots(
            @Parameter(description = "The ID of the transformation")
            @QueryParam("transformationId") Long transformationId) {
        return snapshotService.getLastFiveSnapshots(transformationId);
    }

    @GET
    @Path("/{id}")
    @Operation(
            summary = "Get a snapshot by ID",
            description = "Returns the snapshot with the specified ID, if it exists."
    )
    @APIResponse(
            responseCode = "200",
            description = "The snapshot with the given ID",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = SnapshotDTO.class)
            )
    )
    @APIResponse(
            responseCode = "404",
            description = "No snapshot found with the given ID"
    )
    public SnapshotDTO getById(
            @Parameter(description = "The ID of the snapshot")
            @PathParam("id") String id) {
        return snapshotService.getById(id);
    }
}
