package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.dtos.LogEntryDto;
import de.unistuttgart.stayinsync.monitoring.service.LogService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;

@Path("/logs")
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
            @QueryParam("syncJobId") String syncJobId,
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime,
            @QueryParam("level") @DefaultValue("info") String level
    ) {
        long startNs = Instant.parse(startTime + ":00Z").toEpochMilli() * 1_000_000; // oder DateTimeFormatter
        long endNs   = Instant.parse(endTime + ":00Z").toEpochMilli() * 1_000_000;

        List<LogEntryDto> logs = logService.fetchAndParseLogs(syncJobId, startNs, endNs, level);
        return Response.ok(logs).build();
    }

}

