package de.unistuttgart.stayinsync.pollingnode.execution.controller;

import de.unistuttgart.stayinsync.pollingnode.exceptions.FaultySourceSystemApiRequestMessageDtoException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingJobAlreadyExistsException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingJobExecutionException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingJobNotFoundException;
import de.unistuttgart.stayinsync.pollingnode.execution.executor.PollingJob;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class PollingJobExecutionController {

    private Scheduler scheduler;
    private final Map<Long, JobKey> supportedJobs;

    public PollingJobExecutionController() {
        super();
        this.supportedJobs = new HashMap<>();
    }

    @PostConstruct
    public void init() throws SchedulerException {
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        scheduler = schedulerFactory.getScheduler();
        scheduler.start();
        Log.info("Quartz Scheduler started successfully");
    }

    @PreDestroy
    public void shutdown() throws SchedulerException {
        if (scheduler != null) {
            scheduler.shutdown();
            Log.info("Quartz Scheduler shutdown successfully");
        }
    }

    /**
     * Creates PollingJob with the given information.
     *
     * @param apiRequestConfigurationMessage hold information for creating the PollingJob in future processes
     * @throws PollingJobAlreadyExistsException                if a PollingJob with this id already existed
     * @throws FaultySourceSystemApiRequestMessageDtoException if Exceptions are thrown in called methods
     */
    public void startPollingJobExecution(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage) throws PollingJobAlreadyExistsException, FaultySourceSystemApiRequestMessageDtoException, PollingJobExecutionException {
        if (!pollingJobExists(apiRequestConfigurationMessage.id())) {
            pollingJobCreation(apiRequestConfigurationMessage);
        } else {
            throw new PollingJobAlreadyExistsException("PollingJob Creation failed with this id " + apiRequestConfigurationMessage.id() + "already exists. ");
        }
    }

    /*@
    @ ensures supportedJobs.keySet unchanged
     */

    /**
     * Updates specific PollingJobs in activeJobs Map, by activating, deactivating or changing its info.
     *
     * @param apiRequestConfigurationMessageDTO holds information for updatingProcess
     * @throws PollingJobNotFoundException if activeJobs does not contain the id.
     */
    public void reconfigurePollingJobExecution(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessageDTO) throws PollingJobNotFoundException, PollingJobExecutionException, FaultySourceSystemApiRequestMessageDtoException {
        if (pollingJobExists(apiRequestConfigurationMessageDTO.id())) {

            if (apiRequestConfigurationMessageDTO.active()) {
                pollingJobUpdate(apiRequestConfigurationMessageDTO);
            } else {
                pollingJobDeletion(apiRequestConfigurationMessageDTO.id());
            }
        } else {
            throw new PollingJobNotFoundException("PollingJob Update failed: No PollingJob found with this id " + apiRequestConfigurationMessageDTO.id());
        }
    }

    /**
     * Checks if supportedJobs contains key that equals given key
     *
     * @param id compared to supportedJobs Keys
     * @return true if supportedJobs contained id as key
     */
    public boolean pollingJobExists(final Long id) {
        return supportedJobs.containsKey(id);
    }

    /**
     * Creates new activated or deactivated PollingJob. ID and Created JobKey are added to supportedJobs.
     *
     * @param apiRequestConfigurationMessage used to create Executable PollingJob
     * @throws FaultySourceSystemApiRequestMessageDtoException
     */
    private void pollingJobCreation(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage) throws FaultySourceSystemApiRequestMessageDtoException, PollingJobExecutionException {
        if (!isRequestTypeOnApiTypePossible(apiRequestConfigurationMessage)) {
            throw new FaultySourceSystemApiRequestMessageDtoException("Creation failed for id " + apiRequestConfigurationMessage.id() + ": Use of PUT or POST is not possible on No Spec API´s");
        }

        try {
            final JobDetail job = createJobWithInformation(apiRequestConfigurationMessage);
            supportedJobs.put(apiRequestConfigurationMessage.id(), job.getKey());
            activateJobIfNeeded(apiRequestConfigurationMessage, job);

        } catch (SchedulerException e) {
            Log.error("Failed to schedule polling job for API: " + apiRequestConfigurationMessage.id(), e);
            throw new PollingJobExecutionException("Failed to schedule polling job: " + e);
        }
    }

    /**
     * Activates an already created Job with a newly created Trigger
     *
     * @param apiRequestConfigurationMessage used to call Trigger creation
     * @param job                            is the pollingJob that fits the apiRequestConfigurationMessage
     * @throws SchedulerException
     */
    private void activateJobIfNeeded(SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage, JobDetail job) throws SchedulerException {
            final Trigger trigger = createTriggerWithApiRequestConfigurationMessage(apiRequestConfigurationMessage);
            scheduler.scheduleJob(job, trigger);
            Log.infof("Polling for Job with id %d was activated with timing %d", apiRequestConfigurationMessage.id(), apiRequestConfigurationMessage.pollingIntervallTimeInMs(), job.getKey());
    }

    /**
     * @param apiRequestConfigurationMessage used to compare the values
     * @return true if the Api Type supports the Request Type
     */
    private boolean isRequestTypeOnApiTypePossible(SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage) {
        return !Objects.equals(apiRequestConfigurationMessage.apiConnectionDetails().sourceSystem().apiType(), "No Spec")
                || Objects.equals(apiRequestConfigurationMessage.apiConnectionDetails().endpoint().httpRequestType(), "GET");
    }

    /**
     * Updates PollingJob in supportedJobs with new information and updates Thread if Job is active or changed to deactive. SupportedJobs.keySet stays unchanged.
     *
     * @param apiRequestConfigurationMessage contains information to update PollingJob
     */
    private void pollingJobUpdate(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage) throws FaultySourceSystemApiRequestMessageDtoException, PollingJobExecutionException {
        if (!isRequestTypeOnApiTypePossible(apiRequestConfigurationMessage)) {
            throw new FaultySourceSystemApiRequestMessageDtoException("Creation failed for id " + apiRequestConfigurationMessage.id() + ": Use of PUT or POST is not possible on No Spec API´s");
        }
        try {
            scheduler.deleteJob(supportedJobs.get(apiRequestConfigurationMessage.id()));
            final JobDetail job = createJobWithInformation(apiRequestConfigurationMessage);
            supportedJobs.put(apiRequestConfigurationMessage.id(), job.getKey());
            activateJobIfNeeded(apiRequestConfigurationMessage, job);


        } catch (SchedulerException e) {
            Log.error("Failed to schedule polling job for API: " + apiRequestConfigurationMessage.id(), e);
            throw new PollingJobExecutionException("Failed to schedule polling job: " + e);
        }

    }

    private JobDetail createJobWithInformation(SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage) {

        JobDataMap dataMap = new JobDataMap();
        dataMap.put("requestConfiguration", apiRequestConfigurationMessage);

        return JobBuilder.newJob(PollingJob.class)
                .withIdentity("apiPollingJob-" + apiRequestConfigurationMessage.id())
                .usingJobData(dataMap)
                .build();
    }

    private Trigger createTriggerWithApiRequestConfigurationMessage(SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage) {
        return TriggerBuilder.newTrigger()
                .withIdentity("apiPollingTrigger-" + apiRequestConfigurationMessage.id())
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(apiRequestConfigurationMessage.pollingIntervallTimeInMs())
                        .repeatForever())
                .build();
    }


    /**
     * Deletes PollingJob in supportedJobs and deactivates the JobKey if it was active.
     *
     * @param id used to find the pollingJob to delete.
     */
    private void pollingJobDeletion(final Long id) {
        JobKey jobKey = supportedJobs.remove(id);
        if (jobKey != null) {
            try {
                boolean deleted = scheduler.deleteJob(jobKey);

                if (deleted) {
                    Log.infof("Polling job stopped successfully for API: %s", id);
                } else {
                    Log.warnf("Failed to stop polling job for API: %s - Job not found", id);
                }
            } catch (SchedulerException e) {
                Log.error("Failed to stop polling job for API: " + id, e);
            }
        } else {
            Log.warnf("No active polling job found for API: %s", id);
        }
    }
}