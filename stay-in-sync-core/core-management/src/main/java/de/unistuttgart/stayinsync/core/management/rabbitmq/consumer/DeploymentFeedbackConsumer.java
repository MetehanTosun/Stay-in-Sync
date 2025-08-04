package de.unistuttgart.stayinsync.core.management.rabbitmq.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.service.SourceSystemApiRequestConfigurationService;
import de.unistuttgart.stayinsync.core.configuration.service.TransformationService;
import de.unistuttgart.stayinsync.transport.dto.PollingJobDeploymentFeedbackMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.TransformationDeploymentFeedbackMessageDTO;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@ApplicationScoped
public class DeploymentFeedbackConsumer {
    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    TransformationService transformationService;

    @Inject
    SourceSystemApiRequestConfigurationService sourceSystemApiRequestConfigurationService;

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
            channel.exchangeDeclare("pollingjob-exchange", "direct", true);
            channel.exchangeDeclare("transformation-exchange", "direct", true);


            channel.queueDeclare("transformation-feedback", true, false, false, null);
            channel.queueBind("transformation-feedback", "transformation-exchange", "feedback");

            channel.queueDeclare("pollingjob-feedback", true, false, false, null);
            channel.queueBind("pollingjob-feedback", "pollingjob-exchange", "feedback");

            channel.basicConsume("transformation-feedback", true, processTransformationFeedback(), cancelFeedbackConsumption());
            channel.basicConsume("pollingjob-feedback", true, processPollingJobFeedback(), cancelFeedbackConsumption());
        } catch (IOException e) {
            Log.errorf("Error initialising rabbitMQ message consumer: %s %s", e.getClass(),e.getMessage());
            throw new CoreManagementException("RabbitMQ error", "Could not initiliaze consumer: %s", e.getMessage());
        }
    }

    private CancelCallback cancelFeedbackConsumption() {
        return consumerTag -> {
            Log.warnf("Consumer %s was cancelled from consuming deployment feedbacks", consumerTag);
        };
    }

    /**
     * Processes a message that was rejected by the consumer
     *
     * @return
     */
    private DeliverCallback processTransformationFeedback() {
        return (consumerTag, delivery) -> {
            TransformationDeploymentFeedbackMessageDTO transformationMessageDTO = extractTransformation(delivery);
            Log.infof("Received deployment feedback for transformation (id: %s)", transformationMessageDTO.transformationId());
            transformationService.updateDeploymentStatus(transformationMessageDTO);
        };
    }

    /**
     * Processes a message that was rejected by the consumer
     *
     * @return
     */
    private DeliverCallback processPollingJobFeedback() {
        return (consumerTag, delivery) -> {
            PollingJobDeploymentFeedbackMessageDTO sourceSystemApiRequestConfigurationMessageDTO = extractPollingJob(delivery);
            Log.infof("Received deployment feedback for polling-job id: %s", sourceSystemApiRequestConfigurationMessageDTO.requestConfigId());
            sourceSystemApiRequestConfigurationService.updateDeploymentStatus(sourceSystemApiRequestConfigurationMessageDTO);
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
    private TransformationDeploymentFeedbackMessageDTO extractTransformation(Delivery delivery) throws UnsupportedEncodingException, JsonProcessingException {
        Log.info("Extracting transformation deployment feedback from consumed message");
        String message = new String(delivery.getBody(), "UTF-8");
        return objectMapper.readValue(message, TransformationDeploymentFeedbackMessageDTO.class);
    }

    /**
     * Extracts SourceSystemEndpoint object from delivered message
     *
     * @param delivery
     * @return
     * @throws UnsupportedEncodingException
     * @throws JsonProcessingException
     */
    private PollingJobDeploymentFeedbackMessageDTO extractPollingJob(Delivery delivery) throws UnsupportedEncodingException, JsonProcessingException {
        Log.info("Extracting polling job deployment feedback from consumed message");
        String message = new String(delivery.getBody(), "UTF-8");
        return objectMapper.readValue(message, PollingJobDeploymentFeedbackMessageDTO.class);
    }
}