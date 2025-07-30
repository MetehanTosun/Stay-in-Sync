package de.unistuttgart.stayinsync.pollingnode.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import de.unistuttgart.stayinsync.pollingnode.entities.PollingJobDetails;
import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingNodeException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.rabbitmqexceptions.ConsumerQueueBindingException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.rabbitmqexceptions.ConsumerQueueUnbindingException;
import de.unistuttgart.stayinsync.pollingnode.management.MessageProcessor;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
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
public class PollingJobMessageConsumer {

    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    MessageProcessor pollingJobManagement;

    private Channel channel;

    private String pollingNodeQueueName;

    /**
     * On application startup the application needs to bind queues
     * to the domain specific exchange in order to receive messages
     *
     * @param startupEvent
     */
    void onStart(@Observes StartupEvent startupEvent) {

        try {
            Log.info("Opening rabbitMQ channel");
            channel = rabbitMQClient.connect().openChannel().orElseThrow(() -> new RuntimeException("Unable to establish connection to rabbitMQ"));

            channel.exchangeDeclare("pollingjob-exchange", "direct", true);
            channel.exchangeDeclare("dead-letter-exchange", "direct", true);

            Map<String, Object> queueArgs = new HashMap<>();
            queueArgs.put("x-dead-letter-exchange", "dead-letter-exchange");
            queueArgs.put("x-dead-letter-routing-key", "failed-polling-job");

            channel.queueDeclare("new-pollingjob-queue", true, false, false, queueArgs);
            channel.queueBind("new-pollingjob-queue", "pollingjob-exchange", "");

            //TODO: Consider making queue name, name of pod
            //TODO: Consider different routing key/another queue for dead-letter-exchange
            //Declare queue for a single worker to receive updates on its running jobs
            pollingNodeQueueName = channel.queueDeclare("", false, true, true, queueArgs).getQueue();

            channel.basicConsume("new-pollingjob-queue", false, deployPollingJobCallback(), cancelSyncJobDeploymentCallback("new-pollingjob-queue"));
        } catch (IOException e) {
            Log.errorf("Error initialising rabbitMQ queues", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Processes a message for new polling-job deployments
     *
     * @return
     */
    private DeliverCallback deployPollingJobCallback() {
        return (consumerTag, delivery) -> {
            try {
                SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessageDTO = getPollingJob(delivery);
                Log.infof("Received new request configuration for api: %s", apiRequestConfigurationMessageDTO.apiConnectionDetails().sourceSystem().apiUrl());
                pollingJobManagement.beginSupportOfRequestConfiguration(apiRequestConfigurationMessageDTO);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (PollingNodeException e) {
                Log.error("Failed to process polling-job deployment message");
                //Sending message to dead-letter-exchange
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            }
        };
    }

    /**
     * Serializes message body to {@link SourceSystemApiRequestConfigurationMessageDTO} object
     *
     * @param delivery
     * @return
     * @throws UnsupportedEncodingException
     * @throws JsonProcessingException
     */
    private SourceSystemApiRequestConfigurationMessageDTO getPollingJob(Delivery delivery) throws PollingNodeException {

        try {
            Log.info("Extracting polling-job from consumed message");
            String message = new String(delivery.getBody(), "UTF-8");
            return objectMapper.readValue(message, SourceSystemApiRequestConfigurationMessageDTO.class);
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            throw new PollingNodeException("RabbitMQ error, Unable to extract polling-job from message body");
        }
    }

    /**
     * Called when the consumer got canceled from consuming
     * @return
     */
    private CancelCallback cancelSyncJobDeploymentCallback(String queue) {
        return consumerTag -> {
            Log.warnf("Consumer %s was stopped consuming messages from queue %s", consumerTag, queue);
        };
    }

    /**
     * Processes a message for a running polling-job configuration
     * @return
     */
    private DeliverCallback updateDeployedPollingJobCallback() {
        return (consumerTag, delivery) -> {
            try {
                SourceSystemApiRequestConfigurationMessageDTO apiRequestConfig = getPollingJob(delivery);
                Log.infof("Received update for polling-job %s", apiRequestConfig.apiConnectionDetails().sourceSystem().apiUrl());
                pollingJobManagement.reconfigureSupportOfRequestConfiguration(apiRequestConfig);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (PollingNodeException e) {
                Log.errorf("Failed to process polling-job configuration update message", e);
                //Sending message to dead-letter-exchange
                channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
            }
        };
    }

    /**
     * Adds binding that enables the node to receive information on jobs that are deployed on it
     *
     * @param apiRequestConfigurationMessageDTO
     * @throws PollingNodeException
     */
    public void bindExisitingPollingJobQueue(SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessageDTO) throws ConsumerQueueBindingException {
        try {
            String routingKey = "polling-job" + apiRequestConfigurationMessageDTO.id();
            Log.infof("Binding queue %s with routing key %s", pollingNodeQueueName, routingKey);
            channel.queueBind(pollingNodeQueueName, "pollingjob-exchange", routingKey);
            channel.basicConsume(pollingNodeQueueName, false, updateDeployedPollingJobCallback(), cancelSyncJobDeploymentCallback(pollingNodeQueueName));
        } catch (IOException e) {
            Log.errorf("RabbitMQ error, Failed to bind queue");
            throw new ConsumerQueueBindingException("RabbitMQ error, Failed to bind queue", e);
        }
    }

    /**
     * Unbinds routing key in order to stop listening for updates of a certain job
     *
     * @param apiRequestConfigurationMessageDTO
     * @throws PollingNodeException
     */
    public void unbindExisitingPollingJobQueue(PollingJobDetails apiRequestConfigurationMessageDTO) throws ConsumerQueueUnbindingException {
        try {
            String routingKey = "polling-job" + apiRequestConfigurationMessageDTO.id();
            Log.infof("Unbinding queue %s with routing key %s", pollingNodeQueueName, routingKey);
            channel.queueUnbind(pollingNodeQueueName, "pollingjob-exchange", routingKey);
        } catch (IOException e) {
            throw new ConsumerQueueUnbindingException("RabbitMQ error, Failed to unbind queue", e);
        }
    }


}
