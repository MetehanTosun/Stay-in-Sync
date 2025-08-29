package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.dtos.LogEntryDto;
import de.unistuttgart.stayinsync.monitoring.service.LogService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/api/logs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LogResource {

    private final LogService logService;

    public LogResource(LogService logService) {
        this.logService = logService;
    }

    @GET
    @Path("/")
    public Response getLogs(
            @QueryParam("startTime") long startTime,
            @QueryParam("endTime") long endTime,
            @QueryParam("level") @DefaultValue("info") String level
    ) {
        List<LogEntryDto> logs = logService.fetchAndParseLogs(null, startTime, endTime, level);
        return Response.ok(logs).build();
    }

    /**
     * Neuer Endpunkt: Logs für eine Liste von TransformationIds abrufen
     */
    @POST
    @Path("/transformations")
    public Response getLogsByTransformationIds(
            List<String> transformationIds,
            @QueryParam("startTime") long startTime,
            @QueryParam("endTime") long endTime,
            @QueryParam("level") @DefaultValue("info") String level
    ) {
        List<LogEntryDto> logs = logService.fetchAndParseLogsForTransformations(transformationIds, startTime, endTime, level);
        return Response.ok(logs).build();
    }

    @GET
    @Path("/ErrorSyncJobIds")
    public Response getErrorSyncJobIds(
            @QueryParam("startTime") long startTime,
            @QueryParam("endTime") long endTime
    ) {
        List<String> syncJobIds = logService.fetchErrorSyncJobIds(startTime, endTime);
        return Response.ok(syncJobIds).build();
    }


}

