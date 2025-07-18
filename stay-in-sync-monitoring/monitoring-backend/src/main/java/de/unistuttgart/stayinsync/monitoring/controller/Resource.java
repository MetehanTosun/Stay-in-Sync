package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.service.ErrorLogicService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
public class Resource {

    @Inject
    ErrorLogicService service;

    /**
     * Endpoint for the targeted triggering of an error (for tests)
     * Example: /test?fail=true -> throws a ServiceException
     */
    @GET
    public String triggerErrorSimulation(@QueryParam("fail") boolean fail) {
        service.simulateFailureIfRequested(fail);
        return "{\"status\":\"ok\"}";
    }
}