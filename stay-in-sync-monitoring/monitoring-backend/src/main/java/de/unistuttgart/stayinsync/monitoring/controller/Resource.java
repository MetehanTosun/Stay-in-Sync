package de.unistuttgart.stayinsync.monitoring.controller;

import de.unistuttgart.stayinsync.monitoring.service.ErrorLogicService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

/**
 * REST resource for testing and simulating errors.
 * Provides an endpoint to trigger a simulated error for testing purposes.
 */
@Path("/api/test")
@Produces(MediaType.APPLICATION_JSON)
public class Resource {

    @Inject
    private ErrorLogicService service;

    /**
     * Simulates an error if requested via query parameter.
     * Example: GET /api/test?fail=true will trigger a simulated service exception.
     * @param fail boolean flag indicating whether to simulate an error
     * @return JSON string indicating the status of the operation
     */
    @GET
    public String triggerErrorSimulation(@QueryParam("fail") boolean fail) {
        service.simulateFailureIfRequested(fail);
        return "{\"status\":\"ok\"}";
    }
}