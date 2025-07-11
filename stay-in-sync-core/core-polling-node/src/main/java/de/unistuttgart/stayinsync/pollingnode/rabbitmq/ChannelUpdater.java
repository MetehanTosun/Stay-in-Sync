package de.unistuttgart.stayinsync.pollingnode.rabbitmq;

import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingNodeException;
import de.unistuttgart.stayinsync.pollingnode.usercontrol.management.PollingJobManagement;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ChannelUpdater {

    @Inject
    PollingJobMessageConsumer pollingJobConsumer;
    PollingJobManagement pollingJobManagement;

    public void deployPollingJobExecution(SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessageDTO) throws PollingNodeException {
        Log.infof("Deploying polling for %s at path %s with id %s", apiRequestConfigurationMessageDTO.apiConnectionDetails().sourceSystem().apiUrl(), apiRequestConfigurationMessageDTO.apiConnectionDetails().endpoint().endpointPath(), apiRequestConfigurationMessageDTO.id());
        pollingJobConsumer.bindExisitingPollingJobQueue(apiRequestConfigurationMessageDTO);
        pollingJobManagement.beginSupportOfSourceSystem(apiRequestConfigurationMessageDTO);
    }

    public void reconfigureSyncJobExecution(SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessageDTO) throws PollingNodeException {
        if (!apiRequestConfigurationMessageDTO.active()) {
            Log.infof("Undeploying polling for %s at path %s with id %s", apiRequestConfigurationMessageDTO.apiConnectionDetails().sourceSystem().apiUrl(), apiRequestConfigurationMessageDTO.apiConnectionDetails().endpoint().endpointPath(), apiRequestConfigurationMessageDTO.id());
            pollingJobConsumer.unbindExisitingPollingJobQueue(apiRequestConfigurationMessageDTO);
            pollingJobManagement.reconfigureSupportOfSourceSystem(apiRequestConfigurationMessageDTO);
        } else {
            Log.infof("Updating polling for %s at path %s with id %s", apiRequestConfigurationMessageDTO.apiConnectionDetails().sourceSystem().apiUrl(), apiRequestConfigurationMessageDTO.apiConnectionDetails().endpoint().endpointPath(), apiRequestConfigurationMessageDTO.id());
            pollingJobManagement.reconfigureSupportOfSourceSystem(apiRequestConfigurationMessageDTO);
        }

    }


}
