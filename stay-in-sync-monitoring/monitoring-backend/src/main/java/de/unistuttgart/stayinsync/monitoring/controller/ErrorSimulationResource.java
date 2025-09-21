package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.error.ErrorType;
import de.unistuttgart.stayinsync.monitoring.service.ErrorLogicService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;


/**
 * REST resource for simulating errors based on a given error type.
 * Useful for testing error handling and monitoring pipelines.
 */
@Path("/api/simulate-error")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ErrorSimulationResource {

    private static final Logger LOG = Logger.getLogger(ErrorSimulationResource.class);

    @Inject
    private ErrorLogicService errorLogicService;

    /**
     * Simulates an error based on the provided error type.
     *
     * @param type the type of error to simulate
     * @return JSON response indicating success or failure
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
            return Response.ok(new StatusResponse("ok", "No error triggered")).build();
        } catch (Exception e) {
            LOG.error("Exception occurred while simulating error", e);
            return Response.serverError()
                    .entity(new StatusResponse("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Simple DTO for status responses.
     */
    public static class StatusResponse {

        public String status;
        public String message;

        public StatusResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }
    }
}