package de.unistuttgart.stayinsync.core.configuration.domain.handlers.sync;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemApiRequestConfiguration;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncJob;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.Transformation;
import de.unistuttgart.stayinsync.core.configuration.domain.events.sync.SyncJobPersistedEvent;
import de.unistuttgart.stayinsync.core.configuration.domain.events.sync.SyncJobUpdatedEvent;
import de.unistuttgart.stayinsync.core.configuration.mapping.SourceSystemApiRequestConfigurationFullUpdateMapper;
import de.unistuttgart.stayinsync.core.configuration.mapping.TransformationMapper;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemApiRequestConfigurationService;
import de.unistuttgart.stayinsync.core.management.rabbitmq.producer.PollingJobMessageProducer;
import de.unistuttgart.stayinsync.core.management.rabbitmq.producer.TransformationJobMessageProducer;
import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;
import de.unistuttgart.stayinsync.transport.dto.PollingJobDeploymentFeedbackMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.TransformationMessageDTO;
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
    TransformationJobMessageProducer transformationMessageProducer;

    @Inject
    PollingJobMessageProducer pollingJobMessageProducer;

    @Inject
    TransformationMapper transformationMapper;

    @Inject
    SourceSystemApiRequestConfigurationService sourceSystemApiRequestConfigurationService;

    @Inject
    SourceSystemApiRequestConfigurationFullUpdateMapper apiRequestConfigurationFullUpdateMapper;

    public void onSyncJobPersistedEvent(@Observes SyncJobPersistedEvent event) {
        event.newSyncJob.transformations.forEach(transformation -> deployTransformation(transformation));
    }

    public void onSyncJobUpdatedEvent(@Observes SyncJobUpdatedEvent event) {
        SyncJob outdatedSyncJob = event.outdatedSyncJob;
        SyncJob updatedSyncJob = event.updatedSyncJob;
        Set<Transformation> matchingAndeDeployedTransformations = outdatedSyncJob.transformations.stream()
                .filter(transformation -> updatedSyncJob.transformations.stream()
                        .anyMatch(updatedSyncTransformation -> updatedSyncTransformation.id == transformation.id && !isJobNotDeployed(updatedSyncTransformation)))
                .collect(Collectors.toSet());

        Set<Transformation> undeployedTransformations = updatedSyncJob.transformations.stream()
                .filter(transformation1 -> {
                    return isJobNotDeployed(transformation1);
                }).collect(Collectors.toSet());

        matchingAndeDeployedTransformations.stream().forEach(transformation -> reconfigureSyncJobDeployment(transformation));
        undeployedTransformations.stream().forEach(transformation -> deployTransformation(transformation));
    }

    private void deployTransformation(Transformation transformation) {
        Log.infof("Sending deploy message to worker queue for transformation %s", transformation.name);
        deployNecessaryApiRequestConfigurations(transformation);

        TransformationMessageDTO transformationMessageDTO = transformationMapper.mapToMessageDTO(transformation);
        Log.infof("Transformation '%d' has manifest: %s", transformationMessageDTO.id(), transformationMessageDTO.arcManifest());

        transformationMessageProducer.publishTransformationJob(transformationMessageDTO);
    }


    private void reconfigureSyncJobDeployment(Transformation updatedTransformation) {
        //Deploys the requiered endpoints first
        if (!isJobNotDeployed(updatedTransformation)) {
            Log.infof("Sending reconfiguration message for syncjob %s", updatedTransformation.name);
            deployNecessaryApiRequestConfigurations(updatedTransformation);
            undeployAllUnusedApiRequestConfigurations();

            transformationMessageProducer.reconfigureDeployedTransformationJob(transformationMapper.mapToMessageDTO(updatedTransformation));
        }
    }

    public void deployNecessaryApiRequestConfigurations(Transformation transformation) {
        transformation.sourceSystemApiRequestConfigrations
                .stream()
                .filter(arc -> isJobNotDeployed(arc))
                .forEach(arc -> {
                    sourceSystemApiRequestConfigurationService.updateDeploymentStatus(new PollingJobDeploymentFeedbackMessageDTO(JobDeploymentStatus.DEPLOYING, arc.id, null));
                    deployPollingJob(apiRequestConfigurationFullUpdateMapper.mapToMessageDTO(arc));
                });
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

    private boolean isJobNotDeployed(SourceSystemApiRequestConfiguration requestConfiguration) {
        if(requestConfiguration.deploymentStatus == null)
        {
            return true;
        }
        return !(requestConfiguration.deploymentStatus.equals(JobDeploymentStatus.DEPLOYED) || requestConfiguration.deploymentStatus.equals(JobDeploymentStatus.DEPLOYING));
    }

    private boolean isJobNotDeployed(Transformation transformation) {
        if(transformation.deploymentStatus == null)
        {
            return true;
        }
        return !(transformation.deploymentStatus.equals(JobDeploymentStatus.DEPLOYED) || transformation.deploymentStatus.equals(JobDeploymentStatus.DEPLOYING));
    }

}
