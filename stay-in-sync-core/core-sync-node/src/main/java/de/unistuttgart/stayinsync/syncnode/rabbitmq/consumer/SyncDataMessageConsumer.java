package de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import de.unistuttgart.stayinsync.exception.SyncNodeException;
import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.MDC;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Manages the consumption of synchronization data messages from RabbitMQ.
 * <p>
 * This class serves as the primary bridge between the RabbitMQ messaging infrastructure and the
 * application's business logic. Its responsibilities include:
 * <ul>
 *     <li>Initializing a shared RabbitMQ channel and declaring a central exchange upon application startup.</li>
 *     <li>Dynamically creating and binding queues for specific synchronization jobs when instructed.</li>
 *     <li>Consuming messages, deserializing them into application-specific DTOs.</li>
 *     <li>Passing the data to the {@link DispatcherStateService} to manage transformation state.</li>
 *     <li>Dispatching completed transformation payloads to the {@link TransformationExecutionService} for asynchronous execution.</li>
 *     <li>Handling message acknowledgements (ACK/NACK) to ensure reliable processing.</li>
 * </ul>
 */
@ApplicationScoped
public class SyncDataMessageConsumer {

    // CONSTANTS
    private static final String MDC_TRANSFORMATION_ID_KEY = "transformationId";
    private static final String EXCHANGE_NAME = "sync-data-exchange";
    private static final String EXCHANGE_TYPE = "direct";
    private static final String QUEUE_NAME_PREFIX = "request-config-";
    private static final int PREFETCH_COUNT = 1; // Process one message at a time for fair load distribution.

    // DEPENDENCIES
    private final RabbitMQClient rabbitMQClient;
    private final ObjectMapper objectMapper;
    private final DispatcherStateService dispatcherStateService;
    private final TransformationExecutionService transformationExecutionService;
    private final String queueMaxAge;

    private Channel channel; // Initialized on startup.

    /**
     * Constructs the consumer with its required dependencies using constructor injection.
     * This approach ensures dependencies are explicit, final, and easily mockable for tests.
     *
     * @param rabbitMQClient                 The client for connecting to RabbitMQ.
     * @param objectMapper                   The JSON serializer/deserializer.
     * @param dispatcherStateService         The service that manages the state of incoming sync data.
     * @param transformationExecutionService The service that executes the actual transformation logic.
     * @param queueMaxAge                    The configured time-to-live for dynamically created queues.
     */
    public SyncDataMessageConsumer(RabbitMQClient rabbitMQClient,
                                   ObjectMapper objectMapper,
                                   DispatcherStateService dispatcherStateService,
                                   TransformationExecutionService transformationExecutionService,
                                   @ConfigProperty(name = "stayinsync.rabbitmq.queue.max-age", defaultValue = "1m") String queueMaxAge) {
        this.rabbitMQClient = rabbitMQClient;
        this.objectMapper = objectMapper;
        this.dispatcherStateService = dispatcherStateService;
        this.transformationExecutionService = transformationExecutionService;
        this.queueMaxAge = queueMaxAge;
    }

    /**
     * Initializes the RabbitMQ channel and exchange during application startup.
     * This method ensures that the fundamental messaging infrastructure is ready before any
     * consumers are created or messages are processed. A failure here is fatal to the
     * application's messaging capabilities and will prevent startup.
     *
     * @param startupEvent The Quarkus startup event.
     */
    void onStart(@Observes StartupEvent startupEvent) {
        try {
            Log.info("Initializing RabbitMQ channel and exchange for Sync Data consumption...");
            this.channel = rabbitMQClient.connect()
                    .openChannel()
                    .orElseThrow(() -> new IOException("Failed to open a new RabbitMQ channel."));

            channel.exchangeDeclare(EXCHANGE_NAME, EXCHANGE_TYPE, true);
            channel.basicQos(PREFETCH_COUNT);
            Log.info("RabbitMQ channel and exchange initialized successfully.");
        } catch (IOException e) {
            Log.error("Fatal error during RabbitMQ infrastructure initialization. The application may not function correctly.", e);
            // This re-throw will typically cause the application startup to fail, which is the desired behavior.
            throw new RuntimeException("Could not initialize RabbitMQ channel", e);
        }
    }

    /**
     * Creates a new queue and starts a consumer to listen for sync data messages for a specific job configuration.
     * This method is the entry point for dynamically activating a listener. It declares a dedicated, non-durable
     * stream queue with a max-age (TTL) and binds it to the central exchange.
     *
     * @param config The configuration message containing the ID used to name the queue.
     */
    public void startConsumingSyncData(SourceSystemApiRequestConfigurationMessageDTO config) {
        Objects.requireNonNull(config, "Request configuration message cannot be null");
        final String queueName = QUEUE_NAME_PREFIX + config.id();

        try {
            // Arguments for creating a RabbitMQ Stream queue with a Time-To-Live (TTL).
            Map<String, Object> queueArgs = Map.of(
                    "x-queue-type", "stream",
                    "x-max-age", queueMaxAge
            );

            channel.queueDeclare(queueName, true, false, false, queueArgs);
            Log.infof("Declared queue '%s' with max-age '%s'", queueName, queueMaxAge);

            // The consumer is started with auto-ack disabled to ensure manual acknowledgement.
            channel.basicConsume(queueName, false, deliveryCallback(), cancelCallback());
            Log.infof("Started consuming messages on queue '%s'", queueName);

        } catch (IOException e) {
            Log.errorf(e, "Failed to start consumer on queue '%s'", queueName);
            throw new RuntimeException("Could not start RabbitMQ consumer for queue " + queueName, e);
        }
    }

    /**
     * Factory method for creating the callback that processes incoming messages.
     * This callback encapsulates the main message processing workflow.
     *
     * @return A {@link DeliverCallback} instance.
     */
    private DeliverCallback deliveryCallback() {
        return (consumerTag, delivery) -> processDelivery(delivery);
    }

    /**
     * The core logic for processing a single delivered message from RabbitMQ.
     * It handles deserialization, business logic delegation, and message acknowledgement.
     * If any step fails, the message is negatively acknowledged (NACK'd) to be dead-lettered.
     *
     * @param delivery The raw message delivery from RabbitMQ.
     * @throws IOException If an issue occurs with channel communication (e.g., ack/nack).
     */
    private void processDelivery(Delivery delivery) throws IOException {
        try {
            SyncDataMessageDTO syncData = deserializeMessage(delivery);
            Log.infof("Received sync data for ARC alias: '%s'", syncData.arcAlias());
            Log.debugf("JSON: %s", syncData.jsonData());

            List<ExecutionPayload> completedPayloads = dispatcherStateService.processArc(syncData);

            if (!completedPayloads.isEmpty()) {
                dispatchExecutionPayloads(completedPayloads);
            }

            // Acknowledge the message only after successful processing.
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

        } catch (SyncNodeException e) {
            Log.error("Failed to process sync-data message due to a recoverable business error. NACKing message.", e);
            // NACK the message without requeueing, sending it to the Dead Letter Exchange (if configured).
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
        } catch (Exception e) {
            Log.error("An unexpected error occurred while processing sync-data message. NACKing message.", e);
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
        }
    }

    /**
     * Deserializes the message body from a raw byte array into a {@link SyncDataMessageDTO}.
     *
     * @param delivery The message delivery from RabbitMQ.
     * @return The deserialized {@link SyncDataMessageDTO}.
     * @throws SyncNodeException If the message body is malformed or cannot be decoded.
     */
    private SyncDataMessageDTO deserializeMessage(Delivery delivery) throws SyncNodeException {
        try {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            return objectMapper.readValue(message, SyncDataMessageDTO.class);
        } catch (JsonProcessingException e) {
            throw new SyncNodeException("RabbitMQ Deserialization Error", "Unable to parse sync-job from message body.", e);
        }
    }

    /**
     * Iterates over completed payloads and dispatches them for asynchronous execution.
     *
     * @param payloads A list of payloads ready for transformation.
     */
    private void dispatchExecutionPayloads(List<ExecutionPayload> payloads) {
        for (ExecutionPayload payload : payloads) {
            Long transformationId = payload.job().transformationId();
            try {
                MDC.put(MDC_TRANSFORMATION_ID_KEY, transformationId.toString());
                Log.infof("Dispatching job '%s' for conditional execution.", payload.job().jobId());

                transformationExecutionService.execute(payload)
                        .subscribe().with(
                                result -> handleExecutionSuccess(payload, result),
                                failure -> handleExecutionFailure(payload, failure)
                        );
            } finally {
                // Clean up MDC on the dispatching thread.
                MDC.remove(MDC_TRANSFORMATION_ID_KEY);
            }
        }
    }

    /**
     * Handles the successful completion of a transformation execution.
     * This method is a callback for the reactive stream and may execute on a different thread.
     *
     * @param payload The original payload that was executed.
     * @param result  The result of the execution.
     */
    private void handleExecutionSuccess(ExecutionPayload payload, TransformationResult result) {
        // IMPORTANT: This block runs on a worker thread from the reactive executor.
        // We must re-establish MDC for correct logging context.
        try {
            MDC.put(MDC_TRANSFORMATION_ID_KEY, payload.job().transformationId().toString());
            if (result != null && result.isValidExecution()) {
                Log.infof("Job '%s' completed successfully.", payload.job().jobId());
                Log.debugf("Script transformation output for job '%s': %s", payload.job().jobId(), result.getOutputData());
            } else {
                Log.infof("Job '%s' was skipped by its pre-condition and did not execute.", payload.job().jobId());
            }
        } finally {
            MDC.remove(MDC_TRANSFORMATION_ID_KEY);
        }
    }

    /**
     * Handles the failure of a transformation execution.
     * This method is a callback for the reactive stream and may execute on a different thread.
     *
     * @param payload The original payload that failed.
     * @param failure The throwable that caused the failure.
     */
    private void handleExecutionFailure(ExecutionPayload payload, Throwable failure) {
        // IMPORTANT: This block runs on a worker thread from the reactive executor.
        // We must re-establish MDC for correct logging context.
        try {
            MDC.put(MDC_TRANSFORMATION_ID_KEY, payload.job().transformationId().toString());
            Log.errorf(failure, "Job '%s' failed during execution chain.", payload.job().jobId());
        } finally {
            MDC.remove(MDC_TRANSFORMATION_ID_KEY);
        }
    }

    /**
     * Factory method for the callback that handles consumer cancellation notifications.
     * This is invoked if the consumer is unexpectedly stopped by the RabbitMQ broker.
     *
     * @return A {@link CancelCallback} instance.
     */
    private CancelCallback cancelCallback() {
        return consumerTag -> {
            Log.warnf("Consumer '%s' was cancelled by the broker. It is no longer consuming sync data messages.", consumerTag);
        };
    }
}
