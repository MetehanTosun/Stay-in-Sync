package de.unistuttgart.stayinsync.pollingnode.usercontrol.management;


import de.unistuttgart.stayinsync.pollingnode.execution.controller.PollingJobExecutionController;
import de.unistuttgart.stayinsync.transport.dto.ApiRequestParameterMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.ParamType;
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

    /**
     * Starts SyncJobSupport and handles any unhandled exceptions thrown in the PollingProcess.
     * Also inserts Pathparameters into endpointpath.
     */
    public void beginSupportOfSourceSystem(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage) {
        try {
            if (pollingJobExecutionController.pollingJobExists(apiRequestConfigurationMessage.id())) {
                Log.errorf("There already existed a PollingJob for SourceSystem %s with the id %d", apiRequestConfigurationMessage.apiConnectionDetails().sourceSystem().name(), apiRequestConfigurationMessage.id());
            }
            apiRequestConfigurationMessage.apiConnectionDetails().endpoint().insertPathParametersIntoEndpointPath(apiRequestConfigurationMessage);
            pollingJobExecutionController.startPollingJobExecution(apiRequestConfigurationMessage);
            Log.infof("PollingJob for SourceSystem %s with the id %d was successfully created", apiRequestConfigurationMessage.apiConnectionDetails().sourceSystem().name(), apiRequestConfigurationMessage.id());
        } catch (Exception e) {
            Log.error(e);
        }
    }


    /**
     * Reconfigures SourceSystem if it already exists.
     * Also inserts Pathparameters into endpointpath.
     *
     * @param apiRequestConfigurationMessage contains information for pollingJobDeletion
     */
    public void reconfigureSupportOfSourceSystem(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage) {
        try {
            apiRequestConfigurationMessage.apiConnectionDetails().endpoint().insertPathParametersIntoEndpointPath(apiRequestConfigurationMessage);
            pollingJobExecutionController.reconfigurePollingJobExecution(apiRequestConfigurationMessage);
            Log.infof("PollingJob for SourceSystem %s with the id %d was successfully reconfigured", apiRequestConfigurationMessage.apiConnectionDetails().sourceSystem().name(), apiRequestConfigurationMessage.id());
        }catch(Exception e){
            Log.error(e);
        }
    }

}


