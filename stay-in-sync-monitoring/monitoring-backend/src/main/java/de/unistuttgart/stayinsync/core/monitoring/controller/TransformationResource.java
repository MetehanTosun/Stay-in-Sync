package de.unistuttgart.stayinsync.core.monitoring.controller;



import de.unistuttgart.stayinsync.core.monitoring.service.TransformationService;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringTransformationDto;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.util.List;

@Path("/api/transformation")
public class TransformationResource {

    @Inject
    TransformationService transformationService;

    @GET
    @Path("/{syncJobId}")
    @Operation(summary = "Gibt alle Transformationen für einen SyncJob zurück")
    @APIResponse(
            responseCode = "200",
            description = "Liste der Transformationen für den angegebenen SyncJob",
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
