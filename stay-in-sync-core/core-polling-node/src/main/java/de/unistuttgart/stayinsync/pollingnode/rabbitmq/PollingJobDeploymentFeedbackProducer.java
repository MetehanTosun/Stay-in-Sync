package de.unistuttgart.stayinsync.pollingnode.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingNodeException;
import de.unistuttgart.stayinsync.pollingnode.execution.PollingJobExecutionController;
import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;
import de.unistuttgart.stayinsync.transport.dto.PollingJobDeploymentFeedbackMessageDTO;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;


@ApplicationScoped
public class PollingJobDeploymentFeedbackProducer {

    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    PollingJobExecutionController pollingJobExecutionController;

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
            channel.exchangeDeclare("pollingjob-exchange", "direct", true);
        } catch (Exception e) {
            Log.errorf("Error initialising rabbitMQ message producer: %s %s", e.getClass(), e.getMessage());
            throw new RuntimeException("RabbitMQ error, could not initiliaze producer", e);
        }
    }

    /**
     * Sends a message to confirm successful deployment of a polling job and gives information on which pod the job runs
     * using the HOSTNAME environment variable
     *
     * @param requestConfigId of request config to be confirmed
     */
    public void publishPollingJobFeedback(Long requestConfigId, JobDeploymentStatus jobDeploymentStatus) throws PollingNodeException {
        try {
            Log.infof("Confirming deployment of request config with id: %s", requestConfigId);
            String messageBody = objectMapper.writeValueAsString(new PollingJobDeploymentFeedbackMessageDTO(jobDeploymentStatus, requestConfigId, System.getenv("HOSTNAME")));
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2) // persistent
                    .build();

            channel.basicPublish("pollingjob-exchange", "feedback", properties,
                    messageBody.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new PollingNodeException("Unable to publish Job , Object JSON-serialization failed");
        }
    }

    /**
     * Sends feedback that jobs have been stopped on shutdown
     *
     * @param shutdownEvent
     */
    void onShutdown(@Observes ShutdownEvent shutdownEvent) throws PollingNodeException {
        Set<Long> jobIds = pollingJobExecutionController.getSupportedJobs().keySet();
        for(Long jobId : jobIds)
        {
            publishPollingJobFeedback(jobId, JobDeploymentStatus.STOPPED);
        }
    }

}
