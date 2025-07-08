package de.unistuttgart.stayinsync.pollingnode.usercontrol.management;

import de.unistuttgart.stayinsync.pollingnode.exceptions.FaultySourceSystemApiRequestMessageDtoException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingJobNotFoundException;
import de.unistuttgart.stayinsync.pollingnode.execution.controller.PollingJobExecutionController;
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

    private final PollingJobExecutionController pollingJobExecutionController;

    @Inject
    public PollingJobManagement(final PollingJobExecutionController pollingJobExecutionController) {
        super();
        this.pollingJobExecutionController = pollingJobExecutionController;
    }


    /**
     * Starts SyncJobSupport and handles any unhandled exceptions thrown in the PollingProcess
     */
    public void beginSupportOfSyncJob(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage){
        try {
            if(pollingJobExecutionController.pollingJobExists(apiRequestConfigurationMessage.id())){
                throw new FaultySourceSystemApiRequestMessageDtoException("There already is a open Thread for the given id.");
            }
            pollingJobExecutionController.startPollingJobExecution(apiRequestConfigurationMessage);
            Log.info("PollingJob was created successfully.");
        }catch(FaultySourceSystemApiRequestMessageDtoException e){
            if(pollingJobExecutionController.pollingJobExists(apiRequestConfigurationMessage.id())){
                Log.error("Faulty ApiRequestConfigurationMessage obtained from Core: " + e + ". An unpredictable thread for the given id was created and therefore immediate removed");
            }
            Log.error("Faulty ApiRequestConfigurationMessage obtained from Core: " + e + ". The thread activation was canceled");
        }
    }

    /**
     * Calls PollingJobExecutionController which deletes the PollingJob of the given ApiAddress
     * @param apiRequestConfigurationMessage contains information for pollingJobDeletion
     */
    public void endSupportOfSyncJob(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage){
        try {
            if (pollingJobExecutionController.pollingJobExists(apiRequestConfigurationMessage.id())){
                pollingJobExecutionController.stopPollingJobExecution(apiRequestConfigurationMessage.id());
                Log.info("PollingJob for the source system " + apiRequestConfigurationMessage.sourceSystem() + " with the " + apiRequestConfigurationMessage.endpoint().endpointPath() + " was successfully deleted");
            } else {
                throw new PollingJobNotFoundException("PollingJob for the source system " + apiRequestConfigurationMessage.sourceSystem() + " with the " + apiRequestConfigurationMessage.endpoint().endpointPath() + " that should be deleted does not exist");
            }
        } catch(PollingJobNotFoundException e){
            Log.error(e);
        }
    }

}

