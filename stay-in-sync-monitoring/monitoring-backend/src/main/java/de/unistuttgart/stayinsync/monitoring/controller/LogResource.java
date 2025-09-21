package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.dtos.LogEntryDto;
import de.unistuttgart.stayinsync.monitoring.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.net.URI;
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
    @Operation(
            summary = "Fetch all logs within a given time range and optional log level",
            description = "Returns a list of logs that match the given criteria."
    )
    @APIResponse(
            responseCode = "200",
            description = "List of logs",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = LogEntryDto.class))
    )
    public Response getLogs(
            @Parameter(description = "Start time (epoch millis)", in = ParameterIn.QUERY)
            @QueryParam("startTime") long startTime,
            @Parameter(description = "End time (epoch millis)", in = ParameterIn.QUERY)
            @QueryParam("endTime") long endTime,
            @Parameter(description = "Log level filter (e.g., INFO, ERROR)", in = ParameterIn.QUERY)
            @QueryParam("level") String level
    ) {
        List<LogEntryDto> logs = logService.fetchAndParseLogs(null, startTime, endTime, level);
        return Response.ok(logs).build();
    }

    /**
     * Neuer Endpunkt: Logs für eine Liste von TransformationIds abrufen
     */
    @POST
    @Path("/transformations")
    @Operation(
            summary = "Fetch logs for a list of transformation IDs",
            description = "Returns logs related to the provided transformation IDs within the specified time range and optional log level."
    )
    @APIResponse(
            responseCode = "200",
            description = "List of logs for the given transformation IDs",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = LogEntryDto.class))
    )
    @APIResponse(
            responseCode = "400",
            description = "Invalid request body or parameters"
    )
    @APIResponse(
            responseCode = "201",
            description = "Logs successfully created/fetched",
            headers = @Header(
                    name = HttpHeaders.LOCATION,
                    schema = @Schema(implementation = URI.class)
            )
    )
    public Response getLogsByTransformationIds(
            @Parameter(description = "List of transformation IDs", required = true)
            List<String> transformationIds,
            @Parameter(description = "Start time (epoch millis)", in = ParameterIn.QUERY)
            @QueryParam("startTime") long startTime,
            @Parameter(description = "End time (epoch millis)", in = ParameterIn.QUERY)
            @QueryParam("endTime") long endTime,
            @Parameter(description = "Log level filter (e.g., INFO, ERROR)", in = ParameterIn.QUERY)
            @QueryParam("level") String level
    ) {
        List<LogEntryDto> logs = logService.fetchAndParseLogsForTransformations(transformationIds, startTime, endTime, level);
        return Response.ok(logs).build();
    }


    @GET
    @Path("/service")
    @Operation(
            summary = "Fetch logs for a specific service and optional log level",
            description = "Returns logs for the given service within the specified time range."
    )
    @APIResponse(
            responseCode = "200",
            description = "List of logs for the given service",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = LogEntryDto.class))
    )
    public Response getLogsByService(
            @Parameter(description = "Service name", required = true, in = ParameterIn.QUERY)
            @QueryParam("service") String service,
            @Parameter(description = "Start time (epoch millis)", in = ParameterIn.QUERY)
            @QueryParam("startTime") long startTime,
            @Parameter(description = "End time (epoch millis)", in = ParameterIn.QUERY)
            @QueryParam("endTime") long endTime,
            @Parameter(description = "Log level filter (e.g., INFO, ERROR)", in = ParameterIn.QUERY)
            @QueryParam("level") String level
    ) {
        List<LogEntryDto> logs = logService.fetchAndParseLogsForService(service, startTime, endTime, level);
        return Response.ok(logs).build();
    }

}
