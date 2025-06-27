package de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import de.unistuttgart.stayinsync.exception.SyncNodeException;
import de.unistuttgart.stayinsync.syncnode.syncjob.SyncJobScheduler;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemEndpointMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SyncDataMessageDTO;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@ApplicationScoped
public class SyncDataMessageConsumer {

    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    SyncJobScheduler syncJobScheduler;

    @Inject
    ObjectMapper objectMapper;

    private Channel channel;

    private String syncNodeQueueName;

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
    private CancelCallback cancelSyncDataConsumptionCallback(String queue) {
        return consumerTag -> {
            Log.warnf("Consumer %s was stopped consuming sync data messages from queue %s", consumerTag, queue);
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
                Log.infof("Received syncData for endpoint with id: %s", syncData.endpointId());
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (SyncNodeException e) {
                Log.errorf("Failed to process sync-data message", e);
                //Sending message to dead-letter-exchange
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            }
        };
    }


    public void startConsumingSyncData(SourceSystemEndpointMessageDTO endpointMessageDTO) {
        try {
            channel.basicConsume("endpoint-" + endpointMessageDTO.id(), false, receiveSyncDataCallback(), cancelSyncDataConsumptionCallback(syncNodeQueueName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
