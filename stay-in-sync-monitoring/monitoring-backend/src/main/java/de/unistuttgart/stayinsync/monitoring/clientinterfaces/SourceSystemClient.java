package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringSourceSystemDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * REST client interface for fetching configured monitoring source systems from the backend API.
 * <p>
 * This client uses MicroProfile Rest Client to interact with the backend endpoint.
 */
@RegisterRestClient(configKey = "backend-api")
@Path("/api/config/source-system")
public interface SourceSystemClient {

    /**
     * Retrieves all monitoring source systems configured in the backend.
     *
     * @return a list of MonitoringSourceSystemDto objects representing the source systems
     */
    @GET
    List<MonitoringSourceSystemDto> getAll();
}