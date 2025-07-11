package de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import de.unistuttgart.stayinsync.exception.SyncNodeException;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SyncDataMessageDTO;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class SyncDataMessageConsumer {

    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    ObjectMapper objectMapper;

    private Channel channel;

    /**
     * On application startup the consumer to bind queues to the domain specific exchange in order to
     * start receiving messages
     *
     * @param startupEvent
     */
    void onStart(@Observes StartupEvent startupEvent) {

        try {
            Log.info("Opening rabbitMQ channel");
            channel = rabbitMQClient.connect().openChannel().orElseThrow(() -> new RuntimeException("Unable to establish connection to rabbitMQ"));
            channel.exchangeDeclare("sync-data-exchange", "direct", true);
            channel.basicQos(1);

        } catch (IOException e) {
            Log.errorf("Error initialising rabbitMQ queues", e);
            throw new RuntimeException(e);
        }
    }


    /**
     * Serializes message body to SyncJob object
     *
     * @param delivery
     * @return
     * @throws UnsupportedEncodingException
     * @throws JsonProcessingException
     */
    private SyncDataMessageDTO getSyncDataMessageDTO(Delivery delivery) throws SyncNodeException {

        try {
            Log.info("Extracting sync data message from consumed message");
            String message = new String(delivery.getBody(), "UTF-8");
            return objectMapper.readValue(message, SyncDataMessageDTO.class);
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            throw new SyncNodeException("RabbitMQ error", "Unable to extract sync-job from message body");
        }
    }

    /**
     * Called when the the consumer got canceled from consuming
     *
     * @return
     */
    private CancelCallback cancelSyncDataConsumptionCallback() {
        return consumerTag -> {
            Log.warnf("Consumer %s was stopped consuming sync data messages from queue", consumerTag);
        };
    }

    /**
     * Processes a message for a running sync-job configuration
     *
     * @return
     */
    private DeliverCallback receiveSyncDataCallback() {
        return (consumerTag, delivery) -> {
            try {
                SyncDataMessageDTO syncData = getSyncDataMessageDTO(delivery);
                Log.infof("Received syncData for request config with id: %s", syncData.requestConfigId());
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (SyncNodeException e) {
                Log.errorf("Failed to process sync-data message", e);
                //Sending message to dead-letter-exchange
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            }
        };
    }


    public void startConsumingSyncData(SourceSystemApiRequestConfigurationMessageDTO requestConfigurationMessageDTO) {
        try {
            //TODO: Check if and how it would be possible to not declare queue in both services
            Map<String, Object> queueArgs = new HashMap<>();
            queueArgs.put("x-queue-type", "stream");
            queueArgs.put("x-max-age", "1m");
            channel.queueDeclare("request-config-" + requestConfigurationMessageDTO.id(), true, false, false, Collections.singletonMap("x-queue-type", "stream"));
            channel.basicConsume("request-config-" + requestConfigurationMessageDTO.id(), false, receiveSyncDataCallback(), cancelSyncDataConsumptionCallback());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
