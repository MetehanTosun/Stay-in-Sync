package de.unistuttgart.stayinsync.polling;

import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemApiRequestConfigurationDTO;
import de.unistuttgart.stayinsync.polling.exception.PollingNodeException;
import de.unistuttgart.stayinsync.polling.rabbitmq.PollingJobMessageConsumer;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PollingJobScheduler {

    @Inject
    PollingJobMessageConsumer pollingJobConsumer;

    public void deployPollingJobExecution(SourceSystemApiRequestConfigurationDTO apiRequestConfigurationMessageDTO) throws PollingNodeException {
        Log.infof("Deploying polling for %s at path %s with id %s", apiRequestConfigurationMessageDTO.sourceSystem().apiUrl(), apiRequestConfigurationMessageDTO.endpoint().endpointPath(), apiRequestConfigurationMessageDTO.id());
        pollingJobConsumer.bindExisitingPollingJobQueue(apiRequestConfigurationMessageDTO);
    }

    public void reconfigureSyncJobExecution(SourceSystemApiRequestConfigurationDTO apiRequestConfigurationMessageDTO) throws PollingNodeException {
        if (!apiRequestConfigurationMessageDTO.active()) {
            Log.infof("Undeploying polling for %s at path %s with id %s", apiRequestConfigurationMessageDTO.sourceSystem().apiUrl(), apiRequestConfigurationMessageDTO.endpoint().endpointPath(), apiRequestConfigurationMessageDTO.id());
            pollingJobConsumer.unbindExisitingPollingJobQueue(apiRequestConfigurationMessageDTO);
        } else {
            Log.infof("Updating polling for %s at path %s with id %s", apiRequestConfigurationMessageDTO.sourceSystem().apiUrl(), apiRequestConfigurationMessageDTO.endpoint().endpointPath(), apiRequestConfigurationMessageDTO.id());
        }

    }


}
