package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringSyncJobDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * REST client interface for fetching synchronization jobs for the monitoring graph
 * from the backend API.
 * This client uses MicroProfile Rest Client to interact with the backend endpoint.
 */
@RegisterRestClient(configKey = "backend-api")
@Path("/api/config/sync-job/for-graph")
public interface SyncJobClient {

    /**
     * Retrieves all synchronization jobs available for the monitoring graph.
     *
     * @return a list of MonitoringSyncJobDto objects representing all sync jobs
     */
    @GET
    List<MonitoringSyncJobDto> getAll();

    /**
     * Retrieves a specific synchronization job by its ID.
     *
     * @param id the unique identifier of the sync job
     * @return MonitoringSyncJobDto object representing the sync job
     */
    @GET
    @Path("/{id}")
    MonitoringSyncJobDto getById(@PathParam("id") Long id);
}