package de.unistuttgart.stayinsync.pollingnode.management;

import de.unistuttgart.stayinsync.core.transport.domain.JobDeploymentStatus;
import de.unistuttgart.stayinsync.core.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import de.unistuttgart.stayinsync.pollingnode.entities.PollingJobDetails;
import de.unistuttgart.stayinsync.pollingnode.entities.RequestBuildingDetails;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.InactivePollingJobCreationException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.PollingJobSchedulingException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.management.PollingJobAlreadyExistsException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.management.PollingJobNotFoundException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.rabbitmqexceptions.ConsumerQueueBindingException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.rabbitmqexceptions.ConsumerQueueUnbindingException;
import de.unistuttgart.stayinsync.pollingnode.execution.PollingJobExecutionController;
import de.unistuttgart.stayinsync.pollingnode.rabbitmq.PollingJobDeploymentFeedbackProducer;
import de.unistuttgart.stayinsync.pollingnode.rabbitmq.PollingJobMessageConsumer;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Controls the workflow started by User Interactions in the Frontend.
 * The Goal is to update the PollingJobExecutionController, which executes the PollingJobs, based on these Actions.
 */
@ApplicationScoped
public class MessageProcessor {
    @Inject
    PollingJobExecutionController executionController;

    @Inject
    PollingJobMessageConsumer pollingJobConsumer;

    @Inject
    PollingJobDeploymentFeedbackProducer feedbackProducer;

    /**
     * Starts PollingJobCreationProcess and handles any unhandled exceptions thrown in it.
     *
     * @param apiRequestConfigurationMessage contains data to create PollingJobDetails.
     */
    public void beginSupportOfRequestConfiguration(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage) {
        final PollingJobDetails pollingJobDetails = this.convertSourceSystemApiMessageToPollingJobDetails(apiRequestConfigurationMessage);
        try {
            throwExceptionIfJobDoesExistInExecutionController(pollingJobDetails);
            executionController.pollingJobCreation(pollingJobDetails);
            pollingJobConsumer.bindExisitingPollingJobQueue(apiRequestConfigurationMessage);
            feedbackProducer.publishPollingJobFeedback(pollingJobDetails.id(), JobDeploymentStatus.DEPLOYED);
            Log.infof("PollingJob for SourceSystem %s with the id %d was successfully created", pollingJobDetails.requestBuildingDetails().sourceSystem().name(), apiRequestConfigurationMessage.id());
        } catch (PollingJobAlreadyExistsException e) {
            Log.error("PollingJob " + pollingJobDetails.name() + " with the id " + pollingJobDetails.id() + " did already exist and therefore can´t be created again. Try to reconfigure the existing one instead.", e);
        } catch (InactivePollingJobCreationException e) {
            Log.errorf("It is not possible to create an inactive PollingJob", e);
        } catch (PollingJobSchedulingException e) {
            Log.error("While creating the PollingJob " + pollingJobDetails.name() + " with the id " + pollingJobDetails.id() + "a SchedulingException occurred. " +
                    "Therefore the PollingJob is in a uncontrolled state and should be reconfigured or deleted as soon as possible.", e);
        } catch (ConsumerQueueBindingException e) {
            Log.error("While binding the RabbitMQ PollingJobQueue for the PollingJob with the id " + pollingJobDetails.id() + " an exception occurred.", e);
        }
    }

    /**
     * Reconfigures PollingJob if it already exists.
     * If deploymentStatus is set to RECONFIGURING the existing PollingJob is updated. If deploymentStatus is set to STOPPING the existing PollingJob is deleted.
     *
     * @param apiRequestConfigurationMessage contains data to create PollingJobDetails.
     */
    public void reconfigureSupportOfRequestConfiguration(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage) {
        final PollingJobDetails pollingJobDetails = this.convertSourceSystemApiMessageToPollingJobDetails(apiRequestConfigurationMessage);
        try {
            throwExceptionIfJobDoesNotExistInExecutionController(pollingJobDetails);
            if (apiRequestConfigurationMessage.deploymentStatus().equals(JobDeploymentStatus.RECONFIGURING)) {
                executionController.pollingJobUpdate(pollingJobDetails);
                feedbackProducer.publishPollingJobFeedback(pollingJobDetails.id(), JobDeploymentStatus.DEPLOYED);
                Log.info("PollingJob " + pollingJobDetails.name() + " with the id " + pollingJobDetails.id() + " successfully reconfigured");
            } else if (apiRequestConfigurationMessage.deploymentStatus().equals(JobDeploymentStatus.STOPPING)) {
                executionController.pollingJobDeletion(pollingJobDetails.id());
                feedbackProducer.publishPollingJobFeedback(pollingJobDetails.id(), JobDeploymentStatus.UNDEPLOYED);
                pollingJobConsumer.unbindExisitingPollingJobQueue(pollingJobDetails);
                Log.info("PollingJob " + pollingJobDetails.name() + " with the id " + pollingJobDetails.id() + " successfully removed");
            }
        } catch (PollingJobNotFoundException e) {
            Log.error("PollingJob " + pollingJobDetails.name() + " with the id " + pollingJobDetails.id() + " did not already exist and therefore could not be reconfigured", e);
        } catch (PollingJobSchedulingException e) {
            Log.error("While reconfiguring the PollingJob " + pollingJobDetails.name() + " with the id " + pollingJobDetails.id() + "a SchedulingException occurred. " +
                    "Therefore the PollingJob is in a uncontrolled state and should be reconfigured or deleted as soon as possible.", e);
        } catch (ConsumerQueueUnbindingException e) {
            Log.error("While unbinding the RabbitMQ PollingJobQueue for the PollingJob with the id " + pollingJobDetails.id() + " an exception occurred.", e);
        }
    }

    /**
     * Converts SourceSystemApiRequestConfigurationMessageDTO to PollingJobDetails. The reason for the conversion is Readability only,
     * because these objects are used in the entire component.
     *
     * @param message the object containing all needed details, that is converted in a more readable format.
     * @return PollingJobDetails as result of conversion.
     */
    private PollingJobDetails convertSourceSystemApiMessageToPollingJobDetails(final SourceSystemApiRequestConfigurationMessageDTO message) {
        return new PollingJobDetails(message.name(), message.id(), message.pollingIntervallTimeInMs(), message.workerPodName(),
                new RequestBuildingDetails(message.apiConnectionDetails().sourceSystem(), message.apiConnectionDetails().endpoint(),
                        message.apiConnectionDetails().requestParameters(), message.apiConnectionDetails().requestHeader()));
    }

    /**
     * Throws an Exception if in PollingJobExecutionController there already exists a PollingJob with the same id.
     *
     * @param pollingJobDetails contain the id for checking and are used to give the exception more context as well.
     * @throws PollingJobAlreadyExistsException if the PollingJob already exists in PollingJobExecutionController.
     */
    private void throwExceptionIfJobDoesExistInExecutionController(PollingJobDetails pollingJobDetails) throws PollingJobAlreadyExistsException {
        if (executionController.pollingJobExists(pollingJobDetails.id())) {
            final String exceptionMessage = "There already existed a PollingJob for SourceSystem " + pollingJobDetails.requestBuildingDetails().sourceSystem().name() + " with the id " + pollingJobDetails.id();
            Log.errorf(exceptionMessage, pollingJobDetails.id());
            throw new PollingJobAlreadyExistsException(exceptionMessage, pollingJobDetails.id());
        }
    }

    /**
     * Throws an Exception if in PollingJobExecutionController there does not exist a PollingJob with the same id.
     *
     * @param pollingJobDetails contain the id for checking and are used to give the exception more context as well.
     * @throws PollingJobNotFoundException if the PollingJob does not exist in PollingJobExecutionController.
     */
    private void throwExceptionIfJobDoesNotExistInExecutionController(PollingJobDetails pollingJobDetails) throws PollingJobNotFoundException {
        if (!executionController.pollingJobExists(pollingJobDetails.id())) {
            final String exceptionMessage = "There did not exist a PollingJob for SourceSystem " + pollingJobDetails.requestBuildingDetails().sourceSystem().name() + " with the id " + pollingJobDetails.id();
            Log.errorf(exceptionMessage, pollingJobDetails.id());
            throw new PollingJobNotFoundException(exceptionMessage, pollingJobDetails.id());
        }
    }
}


