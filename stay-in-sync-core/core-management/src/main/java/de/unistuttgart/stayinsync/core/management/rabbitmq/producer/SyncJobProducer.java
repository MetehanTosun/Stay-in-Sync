package de.unistuttgart.stayinsync.core.management.rabbitmq.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SyncJob;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class SyncJobProducer {

    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    ObjectMapper objectMapper;

    private Channel channel;


    void initialize(@Observes StartupEvent startupEvent) {
        try {
            Log.info("Opening rabbitMQ channel");
            channel = rabbitMQClient.connect().openChannel().orElseThrow(() -> new CoreManagementException("RabbitMQ Error", "Unable to open rabbitMQ Channel"));
            channel.exchangeDeclare("syncjob-exchange", "direct", true);
        } catch (Exception e) {
            Log.errorf("Error initialising rabbitMQ message producer", e);
            throw new RuntimeException(e);
        }
    }

    public void publishSyncJob(SyncJob syncJob) throws CoreManagementException {
        try {
            Log.infof("Publishing sync-job %s (id: %s)", syncJob.name, syncJob.id);
            String messageBody = objectMapper.writeValueAsString(syncJob);
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2) // persistent
                    .build();

            channel.basicPublish("syncjob-exchange", "", properties,
                    messageBody.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new CoreManagementException("Unable to publish Job", "Object JSON-serialization failed", e);
        }
    }

    public void reconfigureDeployedSyncJob(SyncJob syncJob) {

    }
}