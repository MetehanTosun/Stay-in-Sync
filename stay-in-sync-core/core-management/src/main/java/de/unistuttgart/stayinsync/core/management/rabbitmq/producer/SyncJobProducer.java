package de.unistuttgart.stayinsync.core.management.rabbitmq.producer;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SyncJob;
import io.quarkus.logging.Log;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

public class SyncJobProducer {

    @Channel("syncjob-requests")
    Emitter<SyncJob> syncJobRequestEmitter;

    public void queueSyncJob(SyncJob syncJob) {
        Log.infof("Emitting syncJob %s ", syncJob.name);
        syncJobRequestEmitter.send(syncJob);

    }

}
