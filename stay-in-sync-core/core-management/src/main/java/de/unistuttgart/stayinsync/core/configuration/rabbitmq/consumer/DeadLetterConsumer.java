package de.unistuttgart.stayinsync.core.configuration.rabbitmq.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import de.unistuttgart.stayinsync.core.transport.dto.TransformationMessageDTO;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.UnsupportedEncodingException;

/**
 * This class is supposed to handle messages from the core-management, that were rejected by the consumers
 */
@ApplicationScoped
public class DeadLetterConsumer {
    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    ObjectMapper objectMapper;

    private Channel channel;

    @ConfigProperty(name = "stayinsync.rabbitmq.enabled", defaultValue = "true")
    boolean rabbitEnabled;

    /**
     * The application needs to bind queues to the domain specific exchanges on startup
     *
     * @param startupEvent
     */
    void initialize(@Observes StartupEvent startupEvent) {

        if (!rabbitEnabled) {
            Log.info("RabbitMQ disabled by config (stayinsync.rabbitmq.enabled=false). Skipping DeadLetterConsumer init.");
            return;
        }

        try {
            Log.info("Opening rabbitMQ channel");
            channel = rabbitMQClient.connect().openChannel().orElseThrow(() -> new RuntimeException("Unable to establish connection to rabbitMQ"));
            channel.exchangeDeclare("dead-letter-exchange", "direct", true);


            channel.queueDeclare("failed-transformations-queue", true, false, false, null);
            channel.queueBind("failed-transformations-queue", "dead-letter-exchange", "failed-transformation-job");

            channel.queueDeclare("failed-pollingjobs-queue", true, false, false, null);
            channel.queueBind("failed-pollingjobs-queue", "dead-letter-exchange", "failed-polling-job");

            channel.basicConsume("failed-transformations-queue", true, processDeadTransformation(), cancelDeadletterConsumption());
            channel.basicConsume("failed-pollingjobs-queue", true, processDeadPollingJob(), cancelDeadletterConsumption());
        } catch (Exception e) {
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
    private DeliverCallback processDeadTransformation() {
        return (consumerTag, delivery) -> {
            TransformationMessageDTO transformationMessageDTO = extractTransformation(delivery);
            Log.warnf("Received dead letter for transformation (id: %s)", transformationMessageDTO.id());
        };
    }

    /**
     * Processes a message that was rejected by the consumer
     *
     * @return
     */
    private DeliverCallback processDeadPollingJob() {
        return (consumerTag, delivery) -> {
            SourceSystemApiRequestConfigurationMessageDTO sourceSystemApiRequestConfigurationMessageDTO = extractPollingJob(delivery);
            Log.warnf("Received dead letter for polling job with id: %s", sourceSystemApiRequestConfigurationMessageDTO.id());
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
    private TransformationMessageDTO extractTransformation(Delivery delivery) throws UnsupportedEncodingException, JsonProcessingException {
        Log.info("Extracting sync-job from consumed message");
        String message = new String(delivery.getBody(), "UTF-8");
        return objectMapper.readValue(message, TransformationMessageDTO.class);
    }

    /**
     * Extracts SourceSystemEndpoint object from delivered message
     *
     * @param delivery
     * @return
     * @throws UnsupportedEncodingException
     * @throws JsonProcessingException
     */
    private SourceSystemApiRequestConfigurationMessageDTO extractPollingJob(Delivery delivery) throws UnsupportedEncodingException, JsonProcessingException {
        Log.info("Extracting Source system endpoint from consumed message");
        String message = new String(delivery.getBody(), "UTF-8");
        return objectMapper.readValue(message, SourceSystemApiRequestConfigurationMessageDTO.class);
    }
}
