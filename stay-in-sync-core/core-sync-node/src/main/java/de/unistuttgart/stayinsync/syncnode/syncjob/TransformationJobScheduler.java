package de.unistuttgart.stayinsync.syncnode.syncjob;

import de.unistuttgart.stayinsync.exception.SyncNodeException;
import de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer.SyncDataMessageConsumer;
import de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer.TransformationJobMessageConsumer;
import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.TransformationMessageDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class TransformationJobScheduler {

    @Inject
    TransformationJobMessageConsumer transformationJobMessageConsumer;

    @Inject
    SyncDataMessageConsumer syncDataMessageConsumer;

    @Inject
    DispatcherStateService dispatcherStateService;

    private Set<TransformationMessageDTO> runningJobs = new HashSet<>();

    public void deployTransformation(TransformationMessageDTO transformation) throws SyncNodeException {
        Log.infof("Deploying transformation with id: %d",transformation.id());

        dispatcherStateService.loadInitialTransformations(transformation);

        transformationJobMessageConsumer.bindSyncJobReconfigurationQueue(transformation);
        consumeJobSyncData(transformation);

        runningJobs.add(transformation);
    }

    public void reconfigureTransformationExecution(TransformationMessageDTO transformation) throws SyncNodeException {
        if (!transformation.jobDeploymentStatus().equals(JobDeploymentStatus.STOPPING)) {
            Log.infof("Undeploy transformation with id %s", transformation.id());
            stopConsumingFromUnusedRequestConfigurations();
            transformationJobMessageConsumer.unbindExisitingSyncJobQueue(transformation);
            runningJobs.remove(transformation);
        } else {
            Log.infof("Updating deployed transformation %s with id %s", transformation.id());
        }
    }

    private void consumeJobSyncData(TransformationMessageDTO transformationMessageDTO) {
        Set<SourceSystemApiRequestConfigurationMessageDTO> requestConfigurations = transformationMessageDTO.requestConfigurationMessageDTOS();
        requestConfigurations.forEach(requestConfig -> {
            Log.infof("Starting to consume data for request-config with id %s", requestConfig.id());
            syncDataMessageConsumer.startConsumingSyncData(requestConfig);
        });
    }

    //TODO implement this method
    private void stopConsumingFromUnusedRequestConfigurations() {

    }

    public Set<TransformationMessageDTO> getRunningJobs() {
        return runningJobs;
    }
}
