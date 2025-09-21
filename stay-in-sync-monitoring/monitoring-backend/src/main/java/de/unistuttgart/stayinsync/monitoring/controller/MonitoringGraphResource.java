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

import java.util.List;

/**
 * REST resource for retrieving the current monitoring graph.
 * Provides an endpoint to fetch the full monitoring graph including nodes and edges.
 */
@Path("/api/monitoringgraph")
@Produces(MediaType.APPLICATION_JSON)
public class MonitoringGraphResource {

    @Inject
    private MonitoringGraphService monitoringGraphService;

    /**
     * Retrieves the current monitoring graph.
     *
     * @return a GraphResponse object containing the nodes and edges of the monitoring graph
     */
    @GET
    @Operation(summary = "Returns the current monitoring graph")
    @APIResponse(
            responseCode = "200",
            description = "Successfully returned the monitoring graph",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
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