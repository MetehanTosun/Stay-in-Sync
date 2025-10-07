// src/main/java/de/unistuttgart/stayinsync/monitoring/core/configuration/rest/ReplayResource.java
package de.unistuttgart.stayinsync.monitoring.core.configuration.rest;

import java.util.Map;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationScriptDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.replay.ReplayExecuteRequestDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.replay.ReplayExecuteResponseDTO;
import de.unistuttgart.stayinsync.monitoring.core.configuration.clients.SnapshotClient;
import de.unistuttgart.stayinsync.monitoring.core.configuration.clients.TransformationScriptClient;
import de.unistuttgart.stayinsync.monitoring.core.configuration.service.ReplayExecutor;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.SnapshotDTO;
import de.unistuttgart.stayinsync.transport.dto.Snapshot.TransformationResultDTO;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/replay")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReplayResource {

    @Inject
    ReplayExecutor executor;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @RestClient
    SnapshotClient snapshotClient;

    @Inject
    @RestClient
    TransformationScriptClient transformationScriptClient;

    // Execute raw payload
    @POST
    @Path("/execute")
    public Response execute(ReplayExecuteRequestDTO req) {
        var result = executor.execute(
                req.scriptName() == null ? "replay.js" : req.scriptName(),
                req.javascriptCode(),
                req.sourceData());

        var resp = new ReplayExecuteResponseDTO(result.outputData(), result.variables(), result.errorInfo());
        return Response.ok(resp).build();
    }

    // Execute a stored snapshot (load snapshot → fetch script → execute)
    @POST
    @Path("/execute/snapshot/{snapshotId}")
    public Response executeSnapshot(@PathParam("snapshotId") String snapshotId) {
        // 1) Load snapshot via SnapshotClient
        SnapshotDTO snap;
        try {
            snap = snapshotClient.byId(snapshotId);
        } catch (WebApplicationException | ProcessingException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(Map.of("error", "Failed to call snapshot service: " + e.getMessage()))
                    .build();
        }

        if (snap == null || snap.getTransformationResult() == null) {
            return Response.status(Response.Status.NOT_FOUND).entity(Map.of("error", "Snapshot not found")).build();
        }

        TransformationResultDTO tr = snap.getTransformationResult();
        if (tr.getTransformationId() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Snapshot missing transformationId")).build();
        }

        // 2) Fetch script by transformationId
        TransformationScriptDTO scriptDto;
        try {
            scriptDto = transformationScriptClient.findByTransformationId(tr.getTransformationId());
        } catch (WebApplicationException | ProcessingException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(Map.of("error", "Failed to call transformation script service: " + e.getMessage()))
                    .build();
        }

        if (scriptDto == null || (scriptDto.javascriptCode() == null && scriptDto.typescriptCode() == null)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Script not found for transformation " + tr.getTransformationId())).build();
        }

        // 3) Ensure we have JS. If only TS present, fail with 501 (you can extend to
        // compile TS→JS later)
        String javascript = scriptDto.javascriptCode() != null ? scriptDto.javascriptCode()
                : null;

        if (javascript == null) {
            return Response.status(Response.Status.NOT_IMPLEMENTED)
                    .entity(Map.of("error", "Transformation only has TypeScript; runtime JS not available"))
                    .build();
        }

        JsonNode source = tr.getSourceData() == null ? objectMapper.createObjectNode() : tr.getSourceData();

        var result = executor.execute(
                "transformation-" + tr.getTransformationId() + ".js",
                javascript,
                source);

        var resp = new ReplayExecuteResponseDTO(result.outputData(), result.variables(), result.errorInfo());
        return Response.ok(resp).build();
    }
}