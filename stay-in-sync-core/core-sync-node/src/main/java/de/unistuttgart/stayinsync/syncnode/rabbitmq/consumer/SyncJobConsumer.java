package de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer;

import de.unistuttgart.stayinsync.syncnode.domain.TransformJob;
import io.quarkus.logging.Log;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class SyncJobConsumer {

    @Incoming("syncjobs")
    public void receiveSyncJob(JsonObject json) {
        TransformJob syncJob = json.mapTo(TransformJob.class);
        Log.infof("Received SyncJob: %s", syncJob.name());
    }

}
