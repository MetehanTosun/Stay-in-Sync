package de.unistuttgart.stayinsync.core.management.rabbitmq.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.transport.dto.TransformationMessageDTO;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * RabbitMQ Message Producer for SyncJobs
 */
@ApplicationScoped
public class TransformationJobMessageProducer {

    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    ObjectMapper objectMapper;

    private Channel channel;

    @ConfigProperty(name = "stayinsync.rabbitmq.enabled", defaultValue = "true")
    boolean rabbitEnabled;

    /**
     * The application needs to open a connection and declare the domain specific exchange on startup
     *
     * @param startupEvent
     */
    void initialize(@Observes StartupEvent startupEvent) {
        try {
            if (!rabbitEnabled) {
                Log.info("RabbitMQ disabled by config (stayinsync.rabbitmq.enabled=false). Skipping SyncJobMessageProducer init.");
                return;
            }
            Log.info("Opening rabbitMQ channel");
            channel = rabbitMQClient.connect().openChannel().orElseThrow(() -> new CoreManagementException("RabbitMQ Error", "Unable to open rabbitMQ Channel"));
            channel.exchangeDeclare("transformation-exchange", "direct", true);
        } catch (Exception e) {
            Log.errorf("Error initialising rabbitMQ message producer: %s %s", e.getClass(),e.getMessage());
            throw new CoreManagementException("RabbitMQ error", "Could not initiliaze producer", e);
        }
    }

    /**
     * Sends a message to the worker queue
     *
     * @param transformationMessageDTO user defined transformation entity
     * @throws CoreManagementException
     */
    public void publishTransformationJob(TransformationMessageDTO transformationMessageDTO) throws CoreManagementException {
        try {
            Log.infof("Publishing sync-job %s (id: %s)", transformationMessageDTO.name(), transformationMessageDTO.id());
            String messageBody = objectMapper.writeValueAsString(transformationMessageDTO);
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2) // persistent
                    .build();

            channel.basicPublish("transformation-exchange", "", properties,
                    messageBody.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new CoreManagementException("Unable to publish Job", "Object JSON-serialization failed", e);
        }
    }

    /**
     * Sends a message to a worker that has bound its queue with that specific routing-key
     *
     * @param syncJob user defined sync-job entity
     * @throws CoreManagementException
     */
    public void reconfigureDeployedTransformationJob(TransformationMessageDTO transformationMessageDTO) {
        try {
            Log.infof("Publishing sync-job %s (id: %s)", transformationMessageDTO.name(), transformationMessageDTO.id());
            String messageBody = objectMapper.writeValueAsString(transformationMessageDTO);
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2) // persistent
                    .build();

            channel.basicPublish("transformation-exchange", "transformation-" + transformationMessageDTO.id(), properties,
                    messageBody.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new CoreManagementException("Unable to publish Job", "Object JSON-serialization failed", e);
        }
    }
}