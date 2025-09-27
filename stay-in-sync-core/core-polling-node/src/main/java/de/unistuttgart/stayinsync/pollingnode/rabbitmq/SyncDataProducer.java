package de.unistuttgart.stayinsync.pollingnode.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import de.unistuttgart.stayinsync.transport.dto.SyncDataMessageDTO;
import de.unistuttgart.stayinsync.pollingnode.entities.PollingJobDetails;
import de.unistuttgart.stayinsync.pollingnode.exceptions.rabbitmqexceptions.ProducerPublishDataException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.rabbitmqexceptions.ProducerSetUpStreamException;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class SyncDataProducer {
    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    ObjectMapper objectMapper;

    private Channel channel;


    void onStart(@Observes StartupEvent startupEvent) {

        try {
            Log.info("Opening rabbitMQ channel");
            channel = rabbitMQClient.connect().openChannel().orElseThrow(() -> new RuntimeException("Unable to establish connection to rabbitMQ"));
            channel.exchangeDeclare("sync-data-exchange", "direct", true);
        } catch (IOException e) {
            Log.errorf("Error initialising rabbitMQ queues", e);
            throw new RuntimeException(e);
        }
    }

    public void setupRequestConfigurationStream(final PollingJobDetails pollingJobDetails) throws ProducerSetUpStreamException {
        try {
            Map<String, Object> queueArgs = new HashMap<>();
            queueArgs.put("x-queue-type", "stream");
            queueArgs.put("x-max-age", "1m");
            channel.queueDeclare("request-config-" + pollingJobDetails.id(), false, false, false, Collections.singletonMap("x-queue-type", "stream"));
            channel.queueBind("request-config-" + pollingJobDetails.id(), "sync-data-exchange", "request-config-" + pollingJobDetails.id());

        } catch (IOException e) {
            final String exceptionMessage = "Unable to setup stream. " + "Failed to setup stream for request config with id: " + pollingJobDetails.id() + e;
            Log.errorf(exceptionMessage, e);
            throw new ProducerSetUpStreamException(exceptionMessage, e);
        }
    }

    public void publishSyncData(SyncDataMessageDTO syncDataMessageDTO) throws ProducerPublishDataException {
        try {
            Log.infof("Publishing data for request-config (id: %s)", syncDataMessageDTO.requestConfigId());
            String messageBody = objectMapper.writeValueAsString(syncDataMessageDTO);
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2) // persistent
                    .build();

            channel.basicPublish("sync-data-exchange", "request-config-" + syncDataMessageDTO.requestConfigId(), properties,
                    messageBody.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            final String exceptionMessage = "Unable to publish Job." + " Object JSON-serialization failed" + e;
            Log.errorf(exceptionMessage, e);
            throw new ProducerPublishDataException(exceptionMessage, e);
        }
    }


}
