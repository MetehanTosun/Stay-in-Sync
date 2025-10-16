// src/main/java/de/unistuttgart/stayinsync/monitoring/core/configuration/rest/ReplayResource.java
package de.unistuttgart.stayinsync.monitoring.core.configuration.rest;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.core.configuration.rest.dtos.replay.ReplayExecuteRequestDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.replay.ReplayExecuteResponseDTO;
import de.unistuttgart.stayinsync.monitoring.core.configuration.clients.SnapshotClient;
import de.unistuttgart.stayinsync.monitoring.core.configuration.clients.TransformationScriptClient;
import de.unistuttgart.stayinsync.monitoring.core.configuration.service.ReplayExecutor;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
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
                req.sourceData(),
                req.generatedSdkCode());

        var resp = new ReplayExecuteResponseDTO(result.outputData(), result.variables(), result.errorInfo());
        return Response.ok(resp).build();
    }
}
// Execute a stored snapshot (load snapshot → fetch script → execute)
