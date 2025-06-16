package de.unistuttgart.stayinsync.core.management.rabbitmq.producer;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SyncJob;
import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;

public class SyncJobProducer {

    @Channel("syncjob-requests")
    Emitter<SyncJob> syncJobRequestEmitter;

    public void deploySyncJob(SyncJob syncJob) {
        Log.infof("Adding new sync-job to worker queue %s, with id %s ", syncJob.name, syncJob.id);
        syncJobRequestEmitter.send(syncJob);
    }

    public void reconfigureDeployedSyncJob(SyncJob syncJob) {
        Log.infof("Sending update message for %s with id %s", syncJob.name, syncJob.id);

        OutgoingRabbitMQMetadata metadata = new OutgoingRabbitMQMetadata.Builder()
                .withRoutingKey("sync-job-" + syncJob.id)
                .build();

        Message<SyncJob> message = Message.of(syncJob, Metadata.of(metadata));

        syncJobRequestEmitter.send(message);
    }
}
