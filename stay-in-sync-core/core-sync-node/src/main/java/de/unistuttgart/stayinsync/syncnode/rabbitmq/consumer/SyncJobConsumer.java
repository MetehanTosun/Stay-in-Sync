package de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer;

import de.unistuttgart.stayinsync.syncnode.domain.SyncJob;
import io.quarkus.logging.Log;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class SyncJobConsumer {

    @Incoming("syncjobs")
    public void receiveSyncJob(JsonObject json) {
        SyncJob syncJob = json.mapTo(SyncJob.class);
        Log.infof("Received SyncJob: %s", syncJob.name());
    }

}
