package de.unistuttgart.stayinsync.core.configuration.domain.handlers.sync;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncJob;
import de.unistuttgart.stayinsync.core.configuration.domain.events.sync.SyncJobPersistedEvent;
import de.unistuttgart.stayinsync.core.configuration.domain.events.sync.SyncJobUpdatedEvent;
import de.unistuttgart.stayinsync.core.management.rabbitmq.producer.PollingJobMessageProducer;
import de.unistuttgart.stayinsync.core.management.rabbitmq.producer.SyncJobMessageProducer;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is supposed to handle all Sync-Job Events
 */
@ApplicationScoped
public class SyncJobEventHandler {

    @Inject
    SyncJobMessageProducer syncJobMessageProducer;

    @Inject
    PollingJobMessageProducer pollingJobMessageProducer;

    public void onSyncJobPersistedEvent(@Observes SyncJobPersistedEvent event) {
        deploySyncJob(event.newSyncJob);
    }

    public void onSyncJobUpdatedEvent(@Observes SyncJobUpdatedEvent event) {
        reconfigureSyncJobDeployment(event.updatedSyncJob, event.outdatedSyncJob);
    }

    private void deploySyncJob(SyncJob syncJob) {
        Log.infof("Sending deploy message to worker queue for syncjob %s", syncJob.name);
        deployAssociatedEndpoints(syncJob);

        syncJobMessageProducer.publishSyncJob(syncJob);
    }


    private void reconfigureSyncJobDeployment(SyncJob updatedSyncJob, SyncJob priorSyncJob) {
        //Deploys the requiered endpoints first
        if (updatedSyncJob.deployed != priorSyncJob.deployed) {
            Log.infof("Sending reconfiguration message for syncjob %s", updatedSyncJob.name);
            deployAssociatedEndpoints(updatedSyncJob);
            undeployAllUnusedPollingJobs();

            syncJobMessageProducer.reconfigureDeployedSyncJob(updatedSyncJob);
        }
    }

    private void deployAssociatedEndpoints(SyncJob syncJob) {
        Set<SourceSystemEndpoint> requieredSyncJobSourceEndpoints = syncJob.transformations.stream().flatMap(transformation -> transformation.sourceSystemEndpoints.stream()).collect(Collectors.toSet());
        Set<SourceSystemEndpoint> unpolledEndpoints = requieredSyncJobSourceEndpoints.stream().filter(sourceSystemEndpoint -> !sourceSystemEndpoint.pollingActive).collect(Collectors.toSet());
        unpolledEndpoints.stream().forEach(sourceSystemEndpoint -> deployPollingJob(sourceSystemEndpoint));
    }

    public void deployPollingJob(SourceSystemEndpoint sourceSystemEndpoint) {
        pollingJobMessageProducer.publishPollingJob(sourceSystemEndpoint);
    }

    public void undeployAllUnusedPollingJobs() {
        List<SourceSystemEndpoint> unusedButPolledEndpoints = SourceSystemEndpoint.listAllWherePollingIsActiveAndUnused();
        unusedButPolledEndpoints.stream().forEach(sourceSystemEndpoint -> {
            sourceSystemEndpoint.pollingActive = false;
            sourceSystemEndpoint.persist();
        });
    }


}
