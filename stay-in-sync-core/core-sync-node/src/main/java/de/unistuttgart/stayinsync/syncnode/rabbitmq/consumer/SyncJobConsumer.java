package de.unistuttgart.stayinsync.syncnode.rabbitmq.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.SyncJob;
import de.unistuttgart.stayinsync.syncnode.syncjob.SyncJobScheduler;
import io.quarkiverse.rabbitmqclient.RabbitMQClient;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.UUID;

@ApplicationScoped
public class SyncJobConsumer {

    @Inject
    RabbitMQClient rabbitMQClient;

    @Inject
    SyncJobScheduler syncJobScheduler;

    private Channel channel;

    private String syncNodeQueueName;

    private String syncNodeConsumerTag;

    void onStart(@Observes StartupEvent startupEvent) {


        try {
            syncNodeConsumerTag = "sync-node-" + UUID.randomUUID();

            Log.info("Opening rabbitMQ channel");
            channel = rabbitMQClient.connect().openChannel().orElseThrow(() -> new RuntimeException("AY"));
            channel.exchangeDeclare("syncjob-exchange", "direct", true);

            channel.queueDeclare("new-syncjob-queue", true, false, false, null);
            channel.queueBind("new-syncjob-queue", "syncjob-exchange", "");

            //Declare queue for single worker to receive updates on deployed jobs
            syncNodeQueueName = channel.queueDeclare().getQueue();

            DeliverCallback deliverCallback = deploySyncJobCallback();

            channel.basicConsume("new-syncjob-queue", true, syncNodeConsumerTag, deliverCallback, cancelSyncJobDeploymentCallback());

        } catch (IOException e) {
            Log.errorf("Error initialising rabbitMQ queues", e);
            throw new RuntimeException(e);
        }
    }

    private DeliverCallback deploySyncJobCallback() {
        return (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            ObjectMapper objectMapper = new ObjectMapper();
            SyncJob syncJob = objectMapper.readValue(message, SyncJob.class);

            Log.infof("Received new sync-job %s", syncJob.name);
            syncJobScheduler.deploySyncJobExecution(syncJob);
        };
    }

    private CancelCallback cancelSyncJobDeploymentCallback() {
        return consumerTag -> {
            Log.warnf("Consumer %s was cancelled", consumerTag);
        };
    }

    private DeliverCallback updateDeployedSyncJobCallback() {
        return (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            ObjectMapper objectMapper = new ObjectMapper();
            SyncJob syncJob = objectMapper.readValue(message, SyncJob.class);

            Log.infof("Received update for sync-job %s", syncJob.name);
            syncJobScheduler.reconfigureSyncJobExecution(syncJob);
        };
    }

    public void bindExisitingSyncJobQueue(SyncJob syncJob) throws IOException {
        String routingKey = "sync-job-" + syncJob.id;
        Log.infof("Binding queue %s with routing key %s", syncNodeQueueName, routingKey);
        channel.queueBind(syncNodeQueueName, "syncjob-exchange", routingKey);
        channel.basicConsume(syncNodeQueueName, true, updateDeployedSyncJobCallback(), cancelSyncJobDeploymentCallback());
    }

    public void unbindExisitingSyncJobQueue(SyncJob syncJob) throws IOException {
        String routingKey = "sync-job-" + syncJob.id;
        Log.infof("Unbinding queue %s with routing key %s", syncNodeQueueName, routingKey);
        channel.queueUnbind(syncNodeQueueName, "syncjob-exchange", routingKey);
    }


}
