package de.unistuttgart.stayinsync.syncnode.syncjob;

import de.unistuttgart.stayinsync.exception.SyncNodeException;
import de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer.SyncDataMessageConsumer;
import de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer.SyncJobMessageConsumer;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SyncJobMessageDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class SyncJobScheduler {

    @Inject
    SyncJobMessageConsumer syncJobMessageConsumer;

    @Inject
    SyncDataMessageConsumer syncDataMessageConsumer;

    @Inject
    DispatcherStateService dispatcherStateService;

    Set<SyncJobMessageDTO> runningJobs = new HashSet<>();

    public void deploySyncJobExecution(SyncJobMessageDTO syncJob) throws SyncNodeException {
        Log.infof("Deploying sync-job %s (id: %s)", syncJob.name(), syncJob.id());

        dispatcherStateService.loadInitialTransformations(syncJob);

        syncJobMessageConsumer.bindSyncJobReconfigurationQueue(syncJob);
        consumeJobSyncData(syncJob);

        runningJobs.add(syncJob);
    }

    public void reconfigureSyncJobExecution(SyncJobMessageDTO syncJob) throws SyncNodeException {
        if (!syncJob.deployed()) {
            Log.infof("Undeploy sync-job %s with id %s", syncJob.name(), syncJob.id());
            stopConsumingFromUnusedRequestConfigurations();
            syncJobMessageConsumer.unbindExisitingSyncJobQueue(syncJob);
            runningJobs.remove(syncJob);
        } else {
            Log.infof("Updating deployed sync-job %s with id %s", syncJob.name(), syncJob.id());
        }
    }

    private void consumeJobSyncData(SyncJobMessageDTO syncJobMessageDTO) {
        Set<SourceSystemApiRequestConfigurationMessageDTO> requestConfigurations = syncJobMessageDTO.transformations().stream() //
                .flatMap(transformationMessageDTO -> transformationMessageDTO.requestConfigurationMessageDTOS().stream()) //
                .collect(Collectors.toSet());
        requestConfigurations.forEach(requestConfig -> {
            Log.infof("Starting to consume data for request-config with id %s", requestConfig.id());
            syncDataMessageConsumer.startConsumingSyncData(requestConfig);
        });
    }

    //TODO implement this method
    private void stopConsumingFromUnusedRequestConfigurations() {

    }
}
