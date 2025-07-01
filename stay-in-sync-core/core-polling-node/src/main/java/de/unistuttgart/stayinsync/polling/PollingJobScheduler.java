package de.unistuttgart.stayinsync.polling;

import de.unistuttgart.stayinsync.polling.exception.PollingNodeException;
import de.unistuttgart.stayinsync.polling.rabbitmq.PollingJobMessageConsumer;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PollingJobScheduler {

    @Inject
    PollingJobMessageConsumer pollingJobConsumer;

    public void deployPollingJobExecution(SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessageDTO) throws PollingNodeException {
        Log.infof("Deploying polling for %s at path %s with id %s", apiRequestConfigurationMessageDTO.sourceSystem().apiUrl(), apiRequestConfigurationMessageDTO.endpoint().endpointPath(), apiRequestConfigurationMessageDTO.id());
        pollingJobConsumer.bindExisitingPollingJobQueue(apiRequestConfigurationMessageDTO);
    }

    public void reconfigureSyncJobExecution(SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessageDTO) throws PollingNodeException {
        if (!apiRequestConfigurationMessageDTO.active()) {
            Log.infof("Undeploying polling for %s at path %s with id %s", apiRequestConfigurationMessageDTO.sourceSystem().apiUrl(), apiRequestConfigurationMessageDTO.endpoint().endpointPath(), apiRequestConfigurationMessageDTO.id());
            pollingJobConsumer.unbindExisitingPollingJobQueue(apiRequestConfigurationMessageDTO);
        } else {
            Log.infof("Updating polling for %s at path %s with id %s", apiRequestConfigurationMessageDTO.sourceSystem().apiUrl(), apiRequestConfigurationMessageDTO.endpoint().endpointPath(), apiRequestConfigurationMessageDTO.id());
        }

    }


}
