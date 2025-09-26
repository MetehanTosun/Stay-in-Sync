package de.unistuttgart.stayinsync.core.configuration.rabbitmq.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * RabbitMQ Message Producer for Polling-Jobs
 */
@ApplicationScoped
public class PollingJobMessageProducer {

    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    ObjectMapper objectMapper;

    private Channel channel;

    /**
     * The application needs to open a connection and declare the domain specific exchange on startup
     *
     * @param startupEvent
     */
    void initialize(@Observes StartupEvent startupEvent) {
        try {
            Log.info("Opening rabbitMQ channel");
            channel = rabbitMQClient.connect().openChannel().orElseThrow(() -> new CoreManagementException("RabbitMQ Error", "Unable to open rabbitMQ Channel"));
            channel.exchangeDeclare("pollingjob-exchange", "direct", true);
        } catch (Exception e) {
            Log.errorf("Error initialising rabbitMQ message producer: %s %s", e.getClass(),e.getMessage());
            throw new CoreManagementException("RabbitMQ error", "Could not initiliaze producer", e);
        }
    }

    public void publishPollingJob(SourceSystemApiRequestConfigurationMessageDTO apiRequestConfiguration) throws CoreManagementException {
        try {
            Log.infof("Publishing job for %s, polling at %s (id: %s)", apiRequestConfiguration.apiConnectionDetails().sourceSystem().apiUrl(),
                    apiRequestConfiguration.apiConnectionDetails().endpoint().endpointPath(), apiRequestConfiguration.id());
            String messageBody = objectMapper.writeValueAsString(apiRequestConfiguration);
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2) // persistent
                    .build();

            channel.basicPublish("pollingjob-exchange", "", properties,
                    messageBody.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new CoreManagementException("Unable to publish Job", "Object JSON-serialization failed", e);
        }
    }

    /**
     * Sends message to a sync node that has bound its queue with the deployed entities routing key
     *
     * @param apiRequestConfiguration
     */
    public void reconfigureDeployedPollingJob(SourceSystemApiRequestConfigurationMessageDTO apiRequestConfiguration) {
        try {
            Log.infof("Publishing job for %s, polling at %s (id: %s)", apiRequestConfiguration.apiConnectionDetails().sourceSystem().apiUrl(),
                    apiRequestConfiguration.apiConnectionDetails().endpoint().endpointPath(), apiRequestConfiguration);
            String messageBody = objectMapper.writeValueAsString(apiRequestConfiguration);
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2) // persistent
                    .build();

            channel.basicPublish("pollingjob-exchange", "polling-job-" + apiRequestConfiguration.id(), properties,
                    messageBody.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new CoreManagementException("Unable to publish Job", "Object JSON-serialization failed", e);
        }
    }
}