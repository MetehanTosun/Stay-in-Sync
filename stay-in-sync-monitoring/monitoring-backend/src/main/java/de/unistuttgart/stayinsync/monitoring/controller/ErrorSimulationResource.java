package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.error.ErrorType;
import de.unistuttgart.stayinsync.monitoring.service.ErrorLogicService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;


//http://localhost:8093/q/swagger-ui/#/

@Path("/api/simulate-error")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ErrorSimulationResource {

    private static final Logger LOG = Logger.getLogger(ErrorSimulationResource.class);

    @Inject
    ErrorLogicService errorLogicService;

    /**
     * Simulates an error based on the type No error triggered
     */
    @GET
    public Response simulate(@QueryParam("type") ErrorType type) {
        if (type == null) {
            LOG.warn("Missing query parameter 'type'");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new StatusResponse("error", "Parameter 'type' is required"))
                    .build();
        }

        LOG.infof("Simulate errors of type: %s", type);

        try {
            errorLogicService.simulateErrorByType(type);
            return Response.ok(new StatusResponse("ok", "No error triggered")).build();
        } catch (Exception e) {
            LOG.error("Error at simulateErrorByType", e);
            return Response.serverError()
                    .entity(new StatusResponse("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Simple status response as JSON object
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
