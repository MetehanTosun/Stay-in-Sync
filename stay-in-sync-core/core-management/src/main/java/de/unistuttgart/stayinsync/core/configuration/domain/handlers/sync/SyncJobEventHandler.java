package de.unistuttgart.stayinsync.core.configuration.domain.handlers.sync;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncJob;
import de.unistuttgart.stayinsync.core.configuration.domain.events.sync.SyncJobPersistedEvent;
import de.unistuttgart.stayinsync.core.configuration.domain.events.sync.SyncJobUpdatedEvent;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemApiRequestConfigurationFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.mapping.SyncJobFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemApiRequestConfigurationService;
import de.unistuttgart.stayinsync.core.management.rabbitmq.producer.PollingJobMessageProducer;
import de.unistuttgart.stayinsync.core.management.rabbitmq.producer.SyncJobMessageProducer;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SyncJobMessageDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * This class is supposed to handle all Sync-Job Events
 */
@ApplicationScoped
public class SyncJobEventHandler {

    @Inject
    SyncJobMessageProducer syncJobMessageProducer;

    @Inject
    PollingJobMessageProducer pollingJobMessageProducer;

    @Inject
    SyncJobFullUpdateMapper syncJobMapper;

    @Inject
    SourceSystemApiRequestConfigurationService sourceSystemApiRequestConfigurationService;

    @Inject
    SourceSystemApiRequestConfigurationFullUpdateMapper apiRequestConfigurationFullUpdateMapper;

    public void onSyncJobPersistedEvent(@Observes SyncJobPersistedEvent event) {
        deploySyncJob(event.newSyncJob);
    }

    public void onSyncJobUpdatedEvent(@Observes SyncJobUpdatedEvent event) {
        reconfigureSyncJobDeployment(event.updatedSyncJob, event.outdatedSyncJob);
    }

    private void deploySyncJob(SyncJob syncJob) {
        Log.infof("Sending deploy message to worker queue for syncjob %s", syncJob.name);
        deployNecessaryApiRequestConfigurations(syncJob);

        SyncJobMessageDTO syncJobMessageDTO = syncJobMapper.mapToMessageDTO(syncJob);
        syncJobMessageDTO.transformations().forEach(transformation -> {
            Log.infof("Transformation '%d' has manifest: %s", transformation.id(), transformation.arcManifest());
        });

        syncJobMessageProducer.publishSyncJob(syncJobMessageDTO);
    }


    private void reconfigureSyncJobDeployment(SyncJob updatedSyncJob, SyncJob priorSyncJob) {
        //Deploys the requiered endpoints first
        if (updatedSyncJob.deployed != priorSyncJob.deployed) {
            Log.infof("Sending reconfiguration message for syncjob %s", updatedSyncJob.name);
            deployNecessaryApiRequestConfigurations(updatedSyncJob);
            undeployAllUnusedApiRequestConfigurations();

            syncJobMessageProducer.reconfigureDeployedSyncJob(syncJobMapper.mapToMessageDTO(updatedSyncJob));
        }
    }

    // Panache doesnt upsert Entity changes in private methods. We set active on inactive ARCs that are to be deployed.
    @Transactional
    public void deployNecessaryApiRequestConfigurations(SyncJob syncJob) {
        /*removed for demo
        Set<SourceSystemApiRequestConfiguration> requieredApiRequestConfigurations = syncJob.transformations.stream().flatMap(transformation -> transformation.sourceSystemApiRequestConfigrations.stream()).collect(Collectors.toSet());
        Set<SourceSystemApiRequestConfiguration> inactiveConfiguration = requieredApiRequestConfigurations.stream().filter(apiRequestConfiguration -> !apiRequestConfiguration.active).collect(Collectors.toSet());
        inactiveConfiguration.stream().forEach(sourceSystemEndpoint -> deployPollingJob(sourceSystemEndpoint));
        List<SourceSystemApiRequestConfiguration> allApiRequestConfigurations = sourceSystemApiRequestConfigurationService.findAllApiRequestConfigurations();
        List<SourceSystemApiRequestConfigurationMessageDTO> messages = allApiRequestConfigurations.stream().map(apiRequestConfiguration -> apiRequestConfigurationFullUpdateMapper.mapToMessageDTO(apiRequestConfiguration)).collect(Collectors.toList());*/
// TODO: PROPOSAL FOR POLLING JOB DEPLOYMENT (fixed required ARCs in script extraction)
        syncJob.transformations.stream()
                .flatMap(transformation -> transformation.sourceSystemApiRequestConfigrations.stream())
                .distinct()
                .filter(arc -> !arc.active)
                .forEach(arc -> {
                    arc.active = true;
                    SourceSystemApiRequestConfigurationMessageDTO message = apiRequestConfigurationFullUpdateMapper.mapToMessageDTO(arc);
                    deployPollingJob(message);
                });

        //messages.stream().forEach(sourceSystemEndpoint -> deployPollingJob(sourceSystemEndpoint));
    }

    public void deployPollingJob(SourceSystemApiRequestConfigurationMessageDTO apiRequestConfiguration) {
        pollingJobMessageProducer.publishPollingJob(apiRequestConfiguration);
    }

    public void undeployAllUnusedApiRequestConfigurations() {
        List<SourceSystemApiRequestConfiguration> unusedButPolledEndpoints = SourceSystemApiRequestConfiguration.listAllWherePollingIsActiveAndUnused();
        unusedButPolledEndpoints.stream().forEach(apiRequestConfiguration -> {
            pollingJobMessageProducer.reconfigureDeployedPollingJob(apiRequestConfigurationFullUpdateMapper.mapToMessageDTO(apiRequestConfiguration));
            apiRequestConfiguration.active = false;
            apiRequestConfiguration.persist();
        });
    }


}
