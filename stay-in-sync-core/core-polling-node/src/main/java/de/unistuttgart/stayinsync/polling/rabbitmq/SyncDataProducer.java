package de.unistuttgart.stayinsync.polling.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import de.unistuttgart.stayinsync.polling.exception.PollingNodeException;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemEndpointMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SyncDataMessageDTO;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class SyncDataProducer {
    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    ObjectMapper objectMapper;

    private Channel channel;


    void onStart(@Observes StartupEvent startupEvent) {

        try {
            Log.info("Opening rabbitMQ channel");
            channel = rabbitMQClient.connect().openChannel().orElseThrow(() -> new RuntimeException("Unable to establish connection to rabbitMQ"));
            channel.exchangeDeclare("sync-data-exchange", "direct", true);
        } catch (IOException e) {
            Log.errorf("Error initialising rabbitMQ queues", e);
            throw new RuntimeException(e);
        }
    }

    void onNewPollingJob(SourceSystemEndpointMessageDTO endpoint) {
        try {
            Map<String, Object> queueArgs = new HashMap<>();
            queueArgs.put("x-queue-type", "stream");
            queueArgs.put("x-max-age", "1m");
            channel.queueDeclare("endpoint-" + endpoint.id() + "-stream", true, false, true, Collections.singletonMap("x-queue-type", "stream"));
            channel.queueBind("endpoint-" + endpoint.id(), "sync-data-exchange", "endpoint-" + endpoint.id());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void publishSyncData(SyncDataMessageDTO syncDataMessageDTO) throws PollingNodeException {
        try {
            Log.infof("Publishing data for endpoint (id: %s)", syncDataMessageDTO.endpointId());
            String messageBody = objectMapper.writeValueAsString(syncDataMessageDTO);
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2) // persistent
                    .build();

            channel.basicPublish("sync-data-exchange", "endpoint-" + syncDataMessageDTO.endpointId(), properties,
                    messageBody.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new PollingNodeException("Unable to publish Job", "Object JSON-serialization failed", e);
        }
    }


}
