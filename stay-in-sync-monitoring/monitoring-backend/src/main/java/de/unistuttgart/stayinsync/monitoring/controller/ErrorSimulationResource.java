package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.error.ErrorType;
import de.unistuttgart.stayinsync.monitoring.service.ErrorLogicService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

/**
 * REST resource for simulating various application errors.
 * Provides endpoints to trigger error scenarios for testing monitoring, logging and alerting pipelines.
 */
@Path("/simulate-error")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ErrorSimulationResource {

    private static final Logger LOG = Logger.getLogger(ErrorSimulationResource.class);

    @Inject
    ErrorLogicService errorLogicService;

    /**
     * Simulates an application error based on the provided error type.
     * Example usage: GET /simulate-error?type=TIMEOUT
     *
     * @param type The type of error to simulate.
     * @return A Response object containing the simulation result.
     */
    @GET
    public Response simulate(@QueryParam("type") ErrorType type) {
        if (type == null) {
            LOG.warn("Missing query parameter 'type'");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new StatusResponse("error", "Parameter 'type' is required"))
                    .build();
        }

        LOG.infof("Simulating error of type: %s", type);

        try {
            errorLogicService.simulateErrorByType(type);
            return Response.ok(new StatusResponse("ok", "Simulated error was handled")).build();
        } catch (Exception e) {
            LOG.error("Exception during error simulation", e);
            return Response.serverError()
                    .entity(new StatusResponse("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Simulates a realistic ScriptEngineException-like scenario without depending on external modules.
     * Useful for monitoring and logging integration tests.
     *
     * @return A Response object indicating the simulated error.
     */
    @POST
    @Path("/realistic-script-error")
    public Response simulateRealisticScriptFailure() {
        String fakeJobId = "sim-job-123";
        String fakeScriptId = "script-abc";

        // Enrich logs with contextual metadata for observability
        MDC.put("jobId", fakeJobId);
        MDC.put("scriptId", fakeScriptId);

        try {
            LOG.infof("Simulating ScriptEngine failure for jobId=%s, scriptId=%s", fakeJobId, fakeScriptId);


            throw new RuntimeException("Script execution failed: Syntax error near unexpected token ';'");

        } catch (RuntimeException e) {
            LOG.errorf(e, "Simulated ScriptEngineException: %s", e.getMessage());

            return Response.serverError()
                    .entity(new StatusResponse("error", "Simulated ScriptEngineException: " + e.getMessage()))
                    .build();
        } finally {
            LOG.infof("Finished simulated ScriptEngine failure for jobId=%s, scriptId=%s", fakeJobId, fakeScriptId);
            MDC.clear();
        }
    }


    public static class StatusResponse {
        public String status;
        public String message;

        public StatusResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }
}
