package de.unistuttgart.stayinsync.monitoring.clientinterfaces;

import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringSyncJobDto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(configKey = "backend-api")
@Path("/api/syncjob")
public interface SyncJobClient {
    @GET
    List<MonitoringSyncJobDto> getAll();
}
