package de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import de.unistuttgart.stayinsync.exception.SyncNodeException;
import de.unistuttgart.stayinsync.syncnode.syncjob.TransformationJobScheduler;
import de.unistuttgart.stayinsync.core.transport.domain.JobDeploymentStatus;
import de.unistuttgart.stayinsync.core.transport.dto.TransformationMessageDTO;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class TransformationJobMessageConsumer {

    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    TransformationJobScheduler transformationJobScheduler;

    @Inject
    TransformationDeploymentFeedbackProducer feedbackProducer;

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

            channel.exchangeDeclare("transformation-exchange", "direct", true);
            channel.exchangeDeclare("dead-letter-exchange", "direct", true);

            Map<String, Object> queueArgs = new HashMap<>();
            queueArgs.put("x-dead-letter-exchange", "dead-letter-exchange");
            queueArgs.put("x-dead-letter-routing-key", "failed-transformation-job");

            channel.queueDeclare("new-transformation-queue", true, false, false, queueArgs);
            channel.queueBind("new-transformation-queue", "transformation-exchange", "");

            //TODO: Consider making queue name, name of pod
            //TODO: Consider different routing key/another queue for dead-letter-exchange
            //Declare queue for a single worker to receive updates on its running jobs
            syncNodeQueueName = channel.queueDeclare("", false, true, true, queueArgs).getQueue();


            channel.basicConsume("new-transformation-queue", false, deploySyncJobCallback(), cancelSyncJobDeploymentCallback("new-transformation-queue"));
        } catch (IOException e) {
            Log.errorf("Error initialising rabbitMQ queues", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Processes a message for new sync-job deployments
     *
     * @return
     */
    private DeliverCallback deploySyncJobCallback() {
        return (consumerTag, delivery) -> {
            try {
                TransformationMessageDTO transformation = getTransformation(delivery);
                Log.infof("Received new transformation %s", transformation.id());
                transformationJobScheduler.deployTransformation(transformation);
                feedbackProducer.publishTransformationFeedback(transformation.id(), JobDeploymentStatus.DEPLOYED);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (SyncNodeException e) {
                Log.error("Failed to process transformation deployment message");
                //Sending message to dead-letter-exchange
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            }
        };
    }

    /**
     * Serializes message body to SyncJob object
     *
     * @param delivery
     * @return
     * @throws UnsupportedEncodingException
     * @throws JsonProcessingException
     */
    private TransformationMessageDTO getTransformation(Delivery delivery) throws SyncNodeException {

        try {
            Log.info("Extracting transformation from consumed message");
            String message = new String(delivery.getBody(), "UTF-8");
            return objectMapper.readValue(message, TransformationMessageDTO.class);
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            throw new SyncNodeException("RabbitMQ error", "Unable to extract transformation from message body");
        }
    }

    /**
     * Called when the the consumer got canceled from consuming
     *
     * @return
     */
    private CancelCallback cancelSyncJobDeploymentCallback(String queue) {
        return consumerTag -> {
            Log.warnf("Consumer %s was stopped consuming messages from queue %s", consumerTag, queue);
        };
    }

    /**
     * Processes a message for a running transformation configuration
     *
     * @return
     */
    private DeliverCallback updateDeployedSyncJobCallback() {
        return (consumerTag, delivery) -> {
            try {
                TransformationMessageDTO transformation = getTransformation(delivery);
                Log.infof("Received update for transformation with id %d", transformation.id());
                JobDeploymentStatus jobDeploymentStatus = transformationJobScheduler.reconfigureTransformationExecution(transformation);
                feedbackProducer.publishTransformationFeedback(transformation.id(), jobDeploymentStatus);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (SyncNodeException e) {
                Log.errorf("Failed to process transformation configuration update message", e);
                //Sending message to dead-letter-exchange
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            }
        };
    }

    /**
     * Adds binding that enables the node to receive information on jobs that are deployed on it
     *
     * @param transformation
     * @throws SyncNodeException
     */
    public void bindSyncJobReconfigurationQueue(TransformationMessageDTO transformation) throws SyncNodeException {
        try {
            String routingKey = "transformation-" + transformation.id();
            Log.infof("Binding queue %s with routing key %s", syncNodeQueueName, routingKey);
            channel.queueBind(syncNodeQueueName, "transformation-exchange", routingKey);
            channel.basicConsume(syncNodeQueueName, false, updateDeployedSyncJobCallback(), cancelSyncJobDeploymentCallback(syncNodeQueueName));
        } catch (IOException e) {
            throw new SyncNodeException("RabbitMQ error", "Failed to bind queue");
        }
    }

    /**
     * Unbinds routing key in order to stop listening for updates of a certain job
     *
     * @param transformation
     * @throws SyncNodeException
     */
    public void unbindExisitingSyncJobQueue(TransformationMessageDTO transformation) throws SyncNodeException {
        try {
            String routingKey = "transformation-" + transformation.id();
            Log.infof("Unbinding queue %s with routing key %s", syncNodeQueueName, routingKey);
            channel.queueUnbind(syncNodeQueueName, "transformation-exchange", routingKey);
        } catch (IOException e) {
            throw new SyncNodeException("RabbitMQ error", "Failed to unbind queue");
        }
    }


}
