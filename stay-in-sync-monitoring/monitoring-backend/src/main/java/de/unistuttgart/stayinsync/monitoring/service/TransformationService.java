package de.unistuttgart.stayinsync.monitoring.service;

import de.unistuttgart.stayinsync.monitoring.clientinterfaces.SyncJobClient;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringSyncJobDto;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringTransformationDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@ApplicationScoped
public class TransformationService {

    @Inject
    @RestClient
    SyncJobClient syncJobClient;

    public List<MonitoringTransformationDto> getTransformations(String syncJobId) {
        MonitoringSyncJobDto syncJob = syncJobClient.getById(Long.valueOf(syncJobId));
        return syncJob.transformations;
    }


}
