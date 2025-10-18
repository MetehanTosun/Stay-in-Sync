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

/**
 * REST resource for replaying transformation scripts either from raw input or
 * from a stored snapshot.
 * <p>
 * Endpoints:
 * <ul>
 * <li><code>POST /api/replay/execute</code> — Execute a provided script and
 * source payload.</li>
 * </ul>
 * This resource coordinates with {@link ReplayExecutor} to run code in a
 * sandboxed GraalJS context and
 * with remote services to fetch snapshots and scripts.
 * </p>
 *
 * @author Mohammed-Ammar Hassnou
 */
@Path("/api/replay")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ReplayResource {

    // Executes JavaScript transform() in a restricted GraalJS context.
    @Inject
    ReplayExecutor executor;

    // Used to create/handle JSON nodes passed to the executor.
    @Inject
    ObjectMapper objectMapper;

    // REST client to retrieve snapshots by id from the snapshot service.
    @Inject
    @RestClient
    SnapshotClient snapshotClient;

    // REST client to lookup transformation scripts by transformation id.
    @Inject
    @RestClient
    TransformationScriptClient transformationScriptClient;

    /**
     * Execute a user-provided JavaScript transformation with the given source data.
     *
     * @endpoint POST /api/replay/execute
     * @param req contains the script name (optional), JavaScript code, source
     *            JSON and the generatedSdkCode
     * @return {@code 200 OK} with {@link ReplayExecuteResponseDTO} including
     *         output, captured variables, and error info
     */
    @POST
    @Path("/execute")
    public Response execute(ReplayExecuteRequestDTO req) {
        // Delegate to ReplayExecutor to run transform() and capture variables.
        var result = executor.execute(
                req.scriptName() == null ? "replay.js" : req.scriptName(),
                req.javascriptCode(),
                req.sourceData(),
                req.generatedSdkCode());
        // Wrap the executor result into the transport DTO for the REST response.
        var resp = new ReplayExecuteResponseDTO(result.outputData(), result.variables(), result.errorInfo());
        return Response.ok(resp).build();
    }
}
