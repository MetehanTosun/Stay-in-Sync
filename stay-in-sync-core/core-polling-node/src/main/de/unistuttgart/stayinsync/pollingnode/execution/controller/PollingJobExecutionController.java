package de.unistuttgart.stayinsync.pollingnode.execution.controller;

import de.unistuttgart.stayinsync.pollingnode.entities.PollingJob;
import de.unistuttgart.stayinsync.pollingnode.execution.service.PollingJobPollingService;

import jakarta.enterprise.context.ApplicationScoped;

import de.unistuttgart.stayinsync.pollingnode.rabbitmq.ProducerSendPolledData;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class PollingJobExecutionController {

    private final PollingJobPollingService pollingJobPollingService;
    private final ProducerSendPolledData producerSendPolledData;
    private final Map<String, PollingJob> supportedPollingJobs;
    private final Map<PollingJob, ScheduledFuture<?>> openPollingJobThreads;
    private final ScheduledExecutorService jobExecutor;

    public PollingJobExecutionController(final PollingJobPollingService pollingJobPollingService, final ProducerSendPolledData producerSendPolledData){
        super();
        this.pollingJobPollingService = pollingJobPollingService;
        this.producerSendPolledData = producerSendPolledData;
        this.supportedPollingJobs = new HashMap<>();
        this.openPollingJobThreads= new HashMap<>();
        this. jobExecutor = Executors.newScheduledThreadPool(10);
    }

    public void startPollingJobExecution(final PollingJob pollingJob){
        supportedPollingJobs.put(pollingJob.getApiAddress(),pollingJob);
        timedPollingJobThreadActivation(pollingJob);
    }

    public void stopPollingJobExecution(final String apiAddress){
        timedPollingJobThreadDeletion(supportedPollingJobs.remove(apiAddress));
        supportedPollingJobs.remove(apiAddress);
    }

    public boolean pollingJobExists(final String apiAddress){
        return supportedPollingJobs.containsKey(apiAddress);
    }

    private void timedPollingJobThreadActivation(final PollingJob pollingJob) {
        Runnable task = () -> {
            producerSendPolledData.send(pollingJobPollingService.pollAndMapData(pollingJob.getApiAddress()));
        };

        ScheduledFuture<?> future = jobExecutor.scheduleAtFixedRate(
                task,
                0,
                1000,
                TimeUnit.MILLISECONDS
        );

        openPollingJobThreads.put(pollingJob, future);
    }

    private void timedPollingJobThreadDeletion(final PollingJob pollingJob) {
        ScheduledFuture<?> future = openPollingJobThreads.remove(pollingJob);
        if (future != null) {
            future.cancel(true);
        }
    }
}
