package de.unistuttgart.stayinsync.core.management.rabbitmq.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SyncJob;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * This class is supposed to handle messages from the core-management, that were rejected by the consumers
 */
public class DeadLetterConsumer {
    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    ObjectMapper objectMapper;

    private Channel channel;

    void initialize(@Observes StartupEvent startupEvent) {

        try {
            Log.info("Opening rabbitMQ channel");
            channel = rabbitMQClient.connect().openChannel().orElseThrow(() -> new RuntimeException("Unable to establish connection to rabbitMQ"));
            channel.exchangeDeclare("dead-letter-exchange", "direct", true);

            channel.queueDeclare("failed-sync-jobs-queue", true, false, false, null);
            channel.queueBind("failed-sync-jobs-queue", "syncjob-exchange", "");

            channel.basicConsume("failed-sync-jobs-queue", true, processDeadLetter(), cancelDeadletterConsumption());
        } catch (IOException e) {
            Log.errorf("Error initialising rabbitMQ queues", e);
            throw new RuntimeException(e);
        }
    }

    private CancelCallback cancelDeadletterConsumption() {
        return consumerTag -> {
            Log.warnf("Consumer %s was cancelled from consuming dead letters", consumerTag);
        };
    }

    /**
     * Processes a message that was rejected by the consumer
     *
     * @return
     */
    private DeliverCallback processDeadLetter() {
        return (consumerTag, delivery) -> {
            SyncJob syncJob = getSyncJob(delivery);
            Log.warnf("Received dead letter for sync-job %s (id: %s)", syncJob.name, syncJob.id);
        };
    }

    /**
     * Extracts Syncjob object from delivered message
     *
     * @param delivery
     * @return
     * @throws UnsupportedEncodingException
     * @throws JsonProcessingException
     */
    private SyncJob getSyncJob(Delivery delivery) throws UnsupportedEncodingException, JsonProcessingException {
        Log.info("Extracting sync-job from consumed message");
        String message = new String(delivery.getBody(), "UTF-8");
        return objectMapper.readValue(message, SyncJob.class);
    }
}
