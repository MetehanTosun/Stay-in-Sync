package de.unistuttgart.stayinsync.polling;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.polling.exception.PollingNodeException;
import de.unistuttgart.stayinsync.polling.rabbitmq.PollingJobMessageConsumer;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PollingJobScheduler {

    @Inject
    PollingJobMessageConsumer pollingJobConsumer;

    public void deployPollingJobExecution(SourceSystemEndpoint sourceSystemEndpoint) throws PollingNodeException {
        Log.infof("Deploying polling for %s at path %s with id %s", sourceSystemEndpoint.sourceSystem.apiUrl, sourceSystemEndpoint.endpointPath, sourceSystemEndpoint.id);
        pollingJobConsumer.bindExisitingPollingJobQueue(sourceSystemEndpoint);
    }

    public void reconfigureSyncJobExecution(SourceSystemEndpoint sourceSystemEndpoint) throws PollingNodeException {
        if (!sourceSystemEndpoint.pollingActive) {
            Log.infof("Undeploying polling for %s at path %s with id %s", sourceSystemEndpoint.sourceSystem.apiUrl, sourceSystemEndpoint.endpointPath, sourceSystemEndpoint.id);
            pollingJobConsumer.unbindExisitingPollingJobQueue(sourceSystemEndpoint);
        } else {
            Log.infof("Updating polling for %s at path %s with id %s", sourceSystemEndpoint.sourceSystem.apiUrl, sourceSystemEndpoint.endpointPath, sourceSystemEndpoint.id);
        }

    }


}
