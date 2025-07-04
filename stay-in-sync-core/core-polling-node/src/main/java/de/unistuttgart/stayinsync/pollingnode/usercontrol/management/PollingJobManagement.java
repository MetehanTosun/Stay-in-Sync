package de.unistuttgart.stayinsync.pollingnode.usercontrol.management;

import de.unistuttgart.stayinsync.pollingnode.entities.SyncJob;
import de.unistuttgart.stayinsync.pollingnode.exceptions.FaultySourceSystemApiRequestMessageDtoException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingJobNotFoundException;
import de.unistuttgart.stayinsync.pollingnode.execution.controller.PollingJobExecutionController;
import de.unistuttgart.stayinsync.pollingnode.usercontrol.configuration.PollingJobConfigurator;
import de.unistuttgart.stayinsync.pollingnode.entities.PollingJob;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Controls the workflow started by User Interactions in the Frontend.
 * The Goal is to update the PollingJobExecutionController, which executes the PollingJobs, based on these Actions.
 */
@ApplicationScoped
public class PollingJobManagement {

    private final PollingJobConfigurator pollingJobConfigurator;
    private final PollingJobExecutionController pollingJobExecutionController;

    @Inject
    public PollingJobManagement(final PollingJobConfigurator pollingJobConfigurator, final PollingJobExecutionController pollingJobExecutionController) {
        super();
        this.pollingJobConfigurator = pollingJobConfigurator;
        this.pollingJobExecutionController = pollingJobExecutionController;
    }


    /**
     * Starts creation Process of a PollingJob and then updates the PollingJobExecutionController with that data
     * @param syncJob contains information needed to create a PollingJob
     */
    public void beginSupportOfSyncJob(final SyncJob syncJob){
        try {
            PollingJob createdPollingJob = pollingJobConfigurator.createPollingJob(syncJob);
            Log.info("PollingJob for the source system" + createdPollingJob.getApiAddress() + " was created successfully.");
            pollingJobExecutionController.startPollingJobExecution(createdPollingJob);
            Log.info("PollingJob was successfully integrated into the polling process.");
        }catch(FaultySourceSystemApiRequestMessageDtoException e){
            Log.error("Faulty SyncJob: " + e + ". No PollingJob was created");
        }
    }

    /**
     * Calls PollingJobExecutionController which deletes the PollingJob of the given ApiAddress
     * @param syncJob contains apiAddress that needs to be deleted
     */
    public void endSupportOfSyncJob(final SyncJob syncJob){
        final String apiAddressOfSyncJob = syncJob.getApiAddress();
        try {
            if (pollingJobExecutionController.pollingJobExists(apiAddressOfSyncJob)){
                pollingJobExecutionController.stopPollingJobExecution(apiAddressOfSyncJob);
                Log.info("PollingJob for the source system " + apiAddressOfSyncJob + " was successfully deleted");
            } else {
                throw new PollingJobNotFoundException("PollingJob for the source system " + apiAddressOfSyncJob + " that should be deleted does not exist");
            }
        } catch(PollingJobNotFoundException e){
            Log.error(e);
        }
    }

}

