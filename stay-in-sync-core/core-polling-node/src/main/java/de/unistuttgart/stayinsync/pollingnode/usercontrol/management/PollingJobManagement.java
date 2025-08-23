package de.unistuttgart.stayinsync.pollingnode.usercontrol.management;


import de.unistuttgart.stayinsync.pollingnode.execution.controller.PollingJobExecutionController;
import de.unistuttgart.stayinsync.pollingnode.rabbitmq.PollingJobDeploymentFeedbackProducer;
import de.unistuttgart.stayinsync.pollingnode.rabbitmq.PollingJobMessageConsumer;
import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Controls the workflow started by User Interactions in the Frontend.
 * The Goal is to update the PollingJobExecutionController, which executes the PollingJobs, based on these Actions.
 */
@ApplicationScoped
public class PollingJobManagement {
    @Inject
    PollingJobExecutionController pollingJobExecutionController;

    @Inject
    PollingJobMessageConsumer pollingJobConsumer;

    @Inject
    PollingJobDeploymentFeedbackProducer feedbackProducer;

    /**
     * Starts SyncJobSupport and handles any unhandled exceptions thrown in the PollingProcess
     */
    public void beginSupportOfRequestConfiguration(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage) {
        try {
            if (pollingJobExecutionController.pollingJobExists(apiRequestConfigurationMessage.id())) {
                Log.errorf("There already existed a PollingJob for SourceSystem %s with the id %d", apiRequestConfigurationMessage.apiConnectionDetails().sourceSystem().name(), apiRequestConfigurationMessage.id());
            }
            Log.infof("Deploying polling for %s at path %s with id %s", apiRequestConfigurationMessage.apiConnectionDetails().sourceSystem().apiUrl(), apiRequestConfigurationMessage.apiConnectionDetails().endpoint().endpointPath(), apiRequestConfigurationMessage.id());
            pollingJobConsumer.bindExisitingPollingJobQueue(apiRequestConfigurationMessage);
            pollingJobExecutionController.startPollingJobExecution(apiRequestConfigurationMessage);
            feedbackProducer.publishPollingJobFeedback(apiRequestConfigurationMessage.id(), JobDeploymentStatus.DEPLOYED);
            Log.infof("PollingJob for SourceSystem %s with the id %d was successfully created", apiRequestConfigurationMessage.apiConnectionDetails().sourceSystem().name(), apiRequestConfigurationMessage.id());
        } catch (Exception e) {
            Log.error(e);
        }
    }


    /**
     * Reconfigures SourceSystem if it already exists
     *
     * @param apiRequestConfigurationMessage contains information for pollingJobDeletion
     */
    public void reconfigureSupportOfRequestConfiguration(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage) {
        try {
            if (apiRequestConfigurationMessage.deploymentStatus().equals(JobDeploymentStatus.STOPPING)) {
                Log.infof("Undeploying polling for %s at path %s with id %s", apiRequestConfigurationMessage.apiConnectionDetails().sourceSystem().apiUrl(), apiRequestConfigurationMessage.apiConnectionDetails().endpoint().endpointPath(), apiRequestConfigurationMessage.id());
                pollingJobConsumer.unbindExisitingPollingJobQueue(apiRequestConfigurationMessage);
                feedbackProducer.publishPollingJobFeedback(apiRequestConfigurationMessage.id(), JobDeploymentStatus.UNDEPLOYED);
            } else if (apiRequestConfigurationMessage.deploymentStatus().equals(JobDeploymentStatus.RECONFIGURING)) {

                Log.infof("Updating polling for %s at path %s with id %s", apiRequestConfigurationMessage.apiConnectionDetails().sourceSystem().apiUrl(), apiRequestConfigurationMessage.apiConnectionDetails().endpoint().endpointPath(), apiRequestConfigurationMessage.id());
                pollingJobExecutionController.reconfigurePollingJobExecution(apiRequestConfigurationMessage);
                feedbackProducer.publishPollingJobFeedback(apiRequestConfigurationMessage.id(), JobDeploymentStatus.DEPLOYED);
            }

            Log.infof("PollingJob for SourceSystem %s with the id %d was successfully reconfigured", apiRequestConfigurationMessage.apiConnectionDetails().sourceSystem().name(), apiRequestConfigurationMessage.id());
        } catch (Exception e) {
            Log.error(e);
        }
    }
}


