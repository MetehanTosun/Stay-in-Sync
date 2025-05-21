package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.error.ErrorType;
import de.unistuttgart.stayinsync.monitoring.service.ErrorLogicService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

//http://localhost:8093/q/swagger-ui/#/

@Path("/simulate-error")
@Produces(MediaType.APPLICATION_JSON)
public class ErrorSimulationResource {

    @Inject
    ErrorLogicService errorLogicService;

    /**
     * Triggers an error of type "ErrorType" for simulation.
     * Example: /simulate-error?type=TIMEOUT
     */
    @GET
    public String simulate(@QueryParam("type") ErrorType type) {
        errorLogicService.simulateErrorByType(type);
        return "{\"status\":\"no-error\"}";
    }
}