package de.unistuttgart.stayinsync.monitoring.controller;



import de.unistuttgart.stayinsync.monitoring.service.TransformationService;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringTransformationDto;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.List;

/**
 * REST resource for fetching transformations related to a SyncJob.
 * Provides endpoints to query all transformations for a given sync job ID.
 */
@Path("/api/transformation")
@Produces(MediaType.APPLICATION_JSON)
public class TransformationResource {

    @Inject
    TransformationService transformationService; // Service for fetching transformation data

    /**
     * GET endpoint to retrieve all transformations for a specific SyncJob.
     *
     * @param syncJobId ID of the sync job
     * @return List of MonitoringTransformationDto representing the transformations
     */
    @GET
    @Path("/{syncJobId}")
    @Operation(summary = "Returns all transformations for a SyncJob")
    @APIResponse(
            responseCode = "200",
            description = "List of transformations for the specified SyncJob",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MonitoringTransformationDto.class),
                    examples = @ExampleObject(
                            name = "transformation-list",
                            value = "[{\"id\":\"1\",\"name\":\"Beispiel\"}]"
                    )
            )
    )
    public List<MonitoringTransformationDto> getTransformations(@PathParam("syncJobId") String syncJobId) {
        return transformationService.getTransformations(syncJobId);
    }
}