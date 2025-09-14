package de.unistuttgart.stayinsync.syncnode.syncjob;

import de.unistuttgart.stayinsync.exception.SyncNodeException;
import de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer.SyncDataMessageConsumer;
import de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer.TransformationJobMessageConsumer;
import de.unistuttgart.stayinsync.core.transport.domain.JobDeploymentStatus;
import de.unistuttgart.stayinsync.core.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import de.unistuttgart.stayinsync.core.transport.dto.TransformationMessageDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.MDC;

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
        try {
            MDC.put("transformationId", transformation.id().toString());
            Log.infof("Deploying transformation with id: %d", transformation.id());

            dispatcherStateService.loadInitialTransformations(transformation);
            transformationJobMessageConsumer.bindSyncJobReconfigurationQueue(transformation);
            consumeJobSyncData(transformation);

            runningJobs.add(transformation);
        } finally {
            MDC.remove("transformationId");
        }
    }

    public JobDeploymentStatus reconfigureTransformationExecution(TransformationMessageDTO transformation) throws SyncNodeException {
        try {
            MDC.put("transformationId", transformation.id().toString());
            if (transformation.deploymentStatus().equals(JobDeploymentStatus.STOPPING)) {
                Log.infof("Undeploy transformation with id %s", transformation.id());
                stopConsumingFromUnusedRequestConfigurations();
                transformationJobMessageConsumer.unbindExisitingSyncJobQueue(transformation);
                runningJobs.remove(transformation);
                return JobDeploymentStatus.UNDEPLOYED;
            } else {
                Log.infof("Updating deployed transformation %s with id %d", transformation.name(), transformation.id());
                return JobDeploymentStatus.DEPLOYED;
            }
        } finally {
            MDC.remove("transformationId");
        }
    }

    private void consumeJobSyncData(TransformationMessageDTO transformationMessageDTO) {
        Set<SourceSystemApiRequestConfigurationMessageDTO> requestConfigurations = transformationMessageDTO.requestConfigurationMessageDTOS();
        requestConfigurations.forEach(requestConfig -> {
            try {
                MDC.put("transformationId", transformationMessageDTO.id().toString());
                Log.infof("Starting to consume data for request-config with id %s", requestConfig.id());
                syncDataMessageConsumer.startConsumingSyncData(requestConfig);
            } finally {
                MDC.remove("transformationId");
            }
        });
    }

    //TODO implement this method
    private void stopConsumingFromUnusedRequestConfigurations() {
    }

    public Set<TransformationMessageDTO> getRunningJobs() {
        return runningJobs;
    }
}
