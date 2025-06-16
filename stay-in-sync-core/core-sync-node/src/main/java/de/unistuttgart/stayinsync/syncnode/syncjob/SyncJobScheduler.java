package de.unistuttgart.stayinsync.syncnode.syncjob;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.SyncJob;
import de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer.SyncJobConsumer;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;

@ApplicationScoped
public class SyncJobScheduler {

    @Inject
    SyncJobConsumer syncJobConsumer;

    public void deploySyncJobExecution(SyncJob syncJob) {
        try {
            syncJobConsumer.bindExisitingSyncJobQueue(syncJob);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reconfigureSyncJobExecution(SyncJob syncJob) {
        if (!syncJob.deployed) {
            Log.infof("Undeploy sync-job %s with id %s", syncJob.name, syncJob.id);

        } else {
            Log.infof("Updating deployed sync-job %s with id %s", syncJob.name, syncJob.id);

        }
    }
}
