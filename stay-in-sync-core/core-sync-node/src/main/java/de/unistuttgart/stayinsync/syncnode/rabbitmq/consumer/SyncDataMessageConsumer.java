package de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import de.unistuttgart.stayinsync.exception.SyncNodeException;
import de.unistuttgart.stayinsync.syncnode.domain.ExecutionPayload;
import de.unistuttgart.stayinsync.syncnode.syncjob.DispatcherStateService;
import de.unistuttgart.stayinsync.syncnode.syncjob.TransformationExecutionService;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SyncDataMessageDTO;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.MDC;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

@ApplicationScoped
public class SyncDataMessageConsumer {

    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    DispatcherStateService dispatcherStateService;

    @Inject
    TransformationExecutionService transformationExecutionService;

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
     * Called when the consumer got canceled from consuming
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
                Log.infof("Received syncData for ARC alias: %s", syncData.arcAlias());
                Log.debugf("JSON: %s", syncData.jsonData());

                List<ExecutionPayload> completedPayloads = dispatcherStateService.processArc(syncData);

                for (ExecutionPayload payload : completedPayloads) {
                    MDC.put("transformationId", payload.job().transformationId().toString());
                    Log.infof("Dispatching job %s for conditional execution", payload.job().jobId());
                    transformationExecutionService.execute(payload)
                            .subscribe().with(
                                    result -> {
                                        MDC.put("transformationId", payload.job().transformationId().toString());
                                        if (result != null) {
                                            Log.infof("Job %s completed successfully: %s", payload.job().jobId(), result.isValidExecution());
                                            Log.infof("Script Transformation payload: %s", result.getOutputData());
                                        } else {
                                            Log.infof("Job %s was skipped by pre-condition and did not run.", payload.job().jobId());
                                        }
                                    },
                                    failure -> {
                                        MDC.put("transformationId", payload.job().transformationId().toString());
                                        Log.errorf(failure, "Job %s failed during execution chain", payload.job().jobId());
                                    }
                            );
                }

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (SyncNodeException e) {
                Log.errorf("Failed to process sync-data message", e);
                // Sending message to dead-letter-exchange
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
            channel.queueDeclare("request-config-" + requestConfigurationMessageDTO.id(), false, false, false, Collections.singletonMap("x-queue-type", "stream"));
            channel.basicConsume("request-config-" + requestConfigurationMessageDTO.id(), false, receiveSyncDataCallback(), cancelSyncDataConsumptionCallback());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
