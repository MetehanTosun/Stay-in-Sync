package de.unistuttgart.stayinsync.core.management.rabbitmq.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncJob;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
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

    /**
     * The application needs to bind queues to the domain specific exchanges on startup
     *
     * @param startupEvent
     */
    void initialize(@Observes StartupEvent startupEvent) {

        try {
            Log.info("Opening rabbitMQ channel");
            channel = rabbitMQClient.connect().openChannel().orElseThrow(() -> new RuntimeException("Unable to establish connection to rabbitMQ"));
            channel.exchangeDeclare("dead-letter-exchange", "direct", true);
            channel.exchangeDeclare("syncjob-exchange", "direct", true);


            channel.queueDeclare("failed-syncjobs-queue", true, false, false, null);
            channel.queueBind("failed-syncjobs-queue", "syncjob-exchange", "failed-sync-job");

            channel.queueDeclare("failed-pollingjobs-queue", true, false, false, null);
            channel.queueBind("failed-pollingjobs-queue", "syncjob-exchange", "failed-polling-job");

            channel.basicConsume("failed-syncjobs-queue", true, processDeadSyncJob(), cancelDeadletterConsumption());
            channel.basicConsume("failed-pollingjobs-queue", true, processDeadPollingJob(), cancelDeadletterConsumption());
        } catch (IOException e) {
            Log.errorf("Error initialising rabbitMQ message consumer: %s %s", e.getClass(),e.getMessage());
            throw new CoreManagementException("RabbitMQ error", "Could not initiliaze consumer: %s", e.getMessage());
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
    private DeliverCallback processDeadSyncJob() {
        return (consumerTag, delivery) -> {
            SyncJob syncJob = extractSyncJob(delivery);
            Log.warnf("Received dead letter for sync-job %s (id: %s)", syncJob.name, syncJob.id);
        };
    }

    /**
     * Processes a message that was rejected by the consumer
     *
     * @return
     */
    private DeliverCallback processDeadPollingJob() {
        return (consumerTag, delivery) -> {
            SyncJob syncJob = extractSyncJob(delivery);
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
    private SyncJob extractSyncJob(Delivery delivery) throws UnsupportedEncodingException, JsonProcessingException {
        Log.info("Extracting sync-job from consumed message");
        String message = new String(delivery.getBody(), "UTF-8");
        return objectMapper.readValue(message, SyncJob.class);
    }

    /**
     * Extracts SourceSystemEndpoint object from delivered message
     *
     * @param delivery
     * @return
     * @throws UnsupportedEncodingException
     * @throws JsonProcessingException
     */
    private SourceSystemEndpoint extractPollingJob(Delivery delivery) throws UnsupportedEncodingException, JsonProcessingException {
        Log.info("Extracting Source system endpoint from consumed message");
        String message = new String(delivery.getBody(), "UTF-8");
        return objectMapper.readValue(message, SourceSystemEndpoint.class);
    }
}
