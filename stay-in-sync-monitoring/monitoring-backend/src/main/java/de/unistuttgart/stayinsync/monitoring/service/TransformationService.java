package de.unistuttgart.stayinsync.monitoring.service;

import de.unistuttgart.stayinsync.monitoring.clientinterfaces.SyncJobClient;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringSyncJobDto;
import de.unistuttgart.stayinsync.transport.dto.monitoringgraph.MonitoringTransformationDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

/**
 * Service for retrieving transformation information that belongs to a specific synchronization job.
 * This service acts as a wrapper around the {@link SyncJobClient}, which communicates
 * with an external system (via REST) to fetch synchronization job details.
 * It provides a convenient method to extract and return the list of
 * {@link MonitoringTransformationDto} objects associated with a given sync job.
 */
@ApplicationScoped
public class TransformationService {

    /**
     * REST client for interacting with the synchronization job service.
     * This client is injected using MicroProfile RestClient and provides
     * methods to query synchronization jobs from an external system.
     */
    @Inject
    @RestClient
    SyncJobClient syncJobClient;

    /**
     * Retrieves the list of transformations for a given synchronization job.
     *
     * @param syncJobId the ID of the synchronization job, provided as a string.
     *                  It will be converted to a {@link Long} before querying the service.
     * @return a list of {@link MonitoringTransformationDto} representing the transformations
     *         defined for the synchronization job.
     * @throws NumberFormatException if the provided syncJobId is not a valid numeric string.
     * @throws RuntimeException if the underlying {@link SyncJobClient} call fails
     *                          or the synchronization job cannot be found.
     */
    public List<MonitoringTransformationDto> getTransformations(String syncJobId) {
        MonitoringSyncJobDto syncJob = syncJobClient.getById(Long.valueOf(syncJobId));
        return syncJob.transformations;
    }
}
