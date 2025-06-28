package de.unistuttgart.stayinsync.pollingnode.rabbitmq;


import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.Map;

import static io.smallrye.config._private.ConfigLogging.log;

@ApplicationScoped
public class ProducerSendPolledData {

    @Inject
    @Channel("polledData")
    Emitter<Map<String,Object>> emitter;

    public void send(final Map<String, Object> createdMap) {
        if(createdMap.isEmpty()) {
            Log.warn("Map was empty and therefore not sent");
        } else {
            log.infof("Map was emitted to RabbitMQ", createdMap);
            emitter.send(createdMap);
        }
    }
}
