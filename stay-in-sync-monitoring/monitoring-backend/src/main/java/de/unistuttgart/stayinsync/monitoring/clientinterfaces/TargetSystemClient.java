package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringTargetSystemDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * REST client interface for fetching configured monitoring target systems from the backend API.
 * Uses MicroProfile Rest Client to interact with the backend endpoint.
 */
@RegisterRestClient(configKey = "backend-api")
@Path("/api/config/target-systems")
public interface TargetSystemClient {

    /**
     * Retrieves all target systems configured in the backend for monitoring.
     *
     * @return a list of MonitoringTargetSystemDto objects representing target systems
     */
    @GET
    List<MonitoringTargetSystemDto> getAll();
}