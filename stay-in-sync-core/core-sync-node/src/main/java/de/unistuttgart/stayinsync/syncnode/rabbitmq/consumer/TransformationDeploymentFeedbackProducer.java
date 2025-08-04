package de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import de.unistuttgart.stayinsync.exception.SyncNodeException;
import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;
import de.unistuttgart.stayinsync.transport.dto.TransformationDeploymentFeedbackMessageDTO;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class TransformationDeploymentFeedbackProducer {

    @ConfigProperty(name = "quarkus.profile")
    String currentProfile;
    
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
            channel = rabbitMQClient.connect().openChannel().orElseThrow(() -> new RuntimeException("Unable to create channel"));
            channel.exchangeDeclare("transformation-exchange", "direct", true);
        } catch (Exception e) {
            Log.errorf("Error initialising rabbitMQ message producer: %s %s", e.getClass(), e.getMessage());
            throw new RuntimeException("RabbitMQ error, could not initiliaze producer", e);
        }
    }

    /**
     * Sends a message to confirm successful deployment of a polling job and gives information on which pod the job runs
     * using the HOSTNAME environment variable
     *
     * @param transformationId of request config to be confirmed
     */
    public void publishTransformationFeedback(Long transformationId, JobDeploymentStatus jobDeploymentStatus) throws SyncNodeException {
        try {
            Log.infof("Confirming deployment of transformation with id: %s", transformationId);

            
            String messageBody = objectMapper.writeValueAsString(getDeploymentFeedback(transformationId, jobDeploymentStatus));
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2) // persistent
                    .build();

            channel.basicPublish("transformation-exchange", "feedback", properties,
                    messageBody.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SyncNodeException("Unable to publish Job" , "Object JSON-serialization failed");
        }
    }

    private TransformationDeploymentFeedbackMessageDTO getDeploymentFeedback(Long transformationId, JobDeploymentStatus jobDeploymentStatus) {

        return new TransformationDeploymentFeedbackMessageDTO(jobDeploymentStatus, transformationId, System.getenv("HOSTNAME")  );
    }

}