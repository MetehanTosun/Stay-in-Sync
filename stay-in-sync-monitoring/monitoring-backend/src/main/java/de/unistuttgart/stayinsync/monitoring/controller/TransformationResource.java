package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.service.TransformationService;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringTransformationDto;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import java.util.List;

@Path("/api/transformation")
public class TransformationResource {

    @Inject
    TransformationService transformationService;

    @Path("/{syncJobId}")
    @GET
    public List<MonitoringTransformationDto> getTransformations(@PathParam("syncJobId") String syncJobId) {
        return transformationService.getTransformations(syncJobId);
    }
}
