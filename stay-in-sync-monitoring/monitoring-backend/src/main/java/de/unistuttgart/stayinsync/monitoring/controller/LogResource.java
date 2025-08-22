package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.dtos.LogEntryDto;
import de.unistuttgart.stayinsync.monitoring.service.LogService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
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

    @GET
    @Path("/SyncJob/{syncJobId}")
    public Response getLogsBySyncJob(
            @PathParam("syncJobId") String syncJobId,
            @QueryParam("startTime") long startTime,
            @QueryParam("endTime") long endTime,
            @QueryParam("level") @DefaultValue("info") String level
    ) {

        List<LogEntryDto> logs = logService.fetchAndParseLogs(syncJobId, startTime, endTime, level);
        return Response.ok(logs).build();
    }

}

