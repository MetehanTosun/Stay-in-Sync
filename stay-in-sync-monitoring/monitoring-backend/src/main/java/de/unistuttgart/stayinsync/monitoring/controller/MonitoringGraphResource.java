package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.service.MonitoringGraphService;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.GraphResponse;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.NodeDto;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;


@Path("/api/monitoringgraph")
@Produces(MediaType.APPLICATION_JSON)
public class MonitoringGraphResource {
    @Inject
    MonitoringGraphService monitoringGraphService;

    @GET
    @Operation(summary = "Gibt den aktuellen Monitoring-Graphen zurück")
    @APIResponse(
            responseCode = "200",
            description = "Erfolgreiche Rückgabe des Monitoring-Graphen",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GraphResponse.class),
                    examples = @ExampleObject(
                            name = "graph-response",
                            value = "{\"nodes\":[{\"id\":\"1\",\"name\":\"Node1\"}],\"edges\":[]}"
                    )
            )
    )
    public GraphResponse getGraph() {
        return monitoringGraphService.buildGraph();
    }
}
