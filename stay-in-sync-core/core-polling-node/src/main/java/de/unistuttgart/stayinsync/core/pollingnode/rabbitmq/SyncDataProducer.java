package de.unistuttgart.stayinsync.core.pollingnode.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import de.unistuttgart.stayinsync.core.pollingnode.exceptions.PollingNodeException;
import de.unistuttgart.stayinsync.core.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import de.unistuttgart.stayinsync.core.transport.dto.SyncDataMessageDTO;
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

    public void setupRequestConfigurationStream(SourceSystemApiRequestConfigurationMessageDTO requestConfiguration) throws PollingNodeException {
        try {
            Map<String, Object> queueArgs = new HashMap<>();
            queueArgs.put("x-queue-type", "stream");
            queueArgs.put("x-max-age", "1m");
            channel.queueDeclare("request-config-" + requestConfiguration.id(), true, false, false, Collections.singletonMap("x-queue-type", "stream"));
            channel.queueBind("request-config-" + requestConfiguration.id(), "sync-data-exchange", "request-config-" + requestConfiguration.id());

        } catch (IOException e) {
            throw new PollingNodeException("Unable to setup stream" + "Failed to setup stream for request config with id: " + requestConfiguration.id() + e);
        }
    }

    public void publishSyncData(SyncDataMessageDTO syncDataMessageDTO) throws PollingNodeException {
        try {
            Log.infof("Publishing data for request-config (id: %s)", syncDataMessageDTO.requestConfigId());
            String messageBody = objectMapper.writeValueAsString(syncDataMessageDTO);
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2) // persistent
                    .build();

            channel.basicPublish("sync-data-exchange", "request-config-" + syncDataMessageDTO.requestConfigId(), properties,
                    messageBody.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new PollingNodeException("Unable to publish Job" + "Object JSON-serialization failed" + e);
        }
    }


}
