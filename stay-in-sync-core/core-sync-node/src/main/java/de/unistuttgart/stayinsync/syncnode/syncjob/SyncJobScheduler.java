package de.unistuttgart.stayinsync.syncnode.syncjob;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SyncJob;
import de.unistuttgart.stayinsync.exception.SyncNodeException;
import de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer.SyncJobConsumer;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SyncJobScheduler {

    @Inject
    SyncJobConsumer syncJobConsumer;

    public void deploySyncJobExecution(SyncJob syncJob) throws SyncNodeException {
        Log.infof("Deploying sync-job %s (id: %s)", syncJob.name, syncJob.id);
        syncJobConsumer.bindExisitingSyncJobQueue(syncJob);
    }

    public void reconfigureSyncJobExecution(SyncJob syncJob) throws SyncNodeException {
        if (!syncJob.deployed) {
            Log.infof("Undeploy sync-job %s with id %s", syncJob.name, syncJob.id);
            syncJobConsumer.unbindExisitingSyncJobQueue(syncJob);
        } else {
            Log.infof("Updating deployed sync-job %s with id %s", syncJob.name, syncJob.id);
        }
    }
}
