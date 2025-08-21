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

import java.util.List;

@Path("/monitoringgraph")
@Produces(MediaType.APPLICATION_JSON)
public class MonitoringGraphResource {
    @Inject
    MonitoringGraphService monitoringGraphService;

    @GET
    public GraphResponse getGraph() {
        return monitoringGraphService.buildGraph();
    }
}
