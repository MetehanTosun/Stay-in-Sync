package de.unistuttgart.stayinsync.pollingnode.execution;

import de.unistuttgart.stayinsync.pollingnode.entities.PollingJobDetails;
import de.unistuttgart.stayinsync.pollingnode.entities.RequestBuildingDetails;
import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingNodeException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.InactivePollingJobCreationException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.PollingJobSchedulingException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.UnsupportedRequestTypeException;
import de.unistuttgart.stayinsync.pollingnode.execution.pollingjob.PollingJob;
import de.unistuttgart.stayinsync.pollingnode.rabbitmq.PollingJobDeploymentFeedbackProducer;
import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class PollingJobExecutionController {

    @Inject
    PollingJobDeploymentFeedbackProducer feedbackProducer;

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

    public Map<Long, JobKey> getSupportedJobs() {
        return supportedJobs;
    }


    /**
     * Creates new PollingJob, that is scheduled and referenced in supportedJobs with its id and JobKey
     *
     * @param pollingJobDetails contains data to create the PollingJob
     * @throws UnsupportedRequestTypeException     if requestType is not possible on ApiType.
     * @throws InactivePollingJobCreationException if something went wrong at the PollingJob scheduling step.
     */
    public void pollingJobCreation(final PollingJobDetails pollingJobDetails) throws UnsupportedRequestTypeException, InactivePollingJobCreationException, PollingJobSchedulingException {
        throwUnsupportedRequestTypeExceptionIfRequestTypeDoesNotFitApiType(pollingJobDetails);
        try {
            scheduleJob(pollingJobDetails);
        } catch (SchedulerException e) {
            final String exceptionMessage = "Failed to schedule polling job " + pollingJobDetails.name() + " with the id " + pollingJobDetails.id();
            Log.errorf(exceptionMessage, e);
            throw new PollingJobSchedulingException(exceptionMessage, e);
        }
    }

    /**
     * Reschedules PollingJob and updates scheduledJobs with updated information.
     *
     * @param pollingJobDetails contains data to create the PollingJob.
     * @throws UnsupportedRequestTypeException if requestType is not possible on ApiType.
     * @throws PollingJobSchedulingException   if something went wrong at the PollingJob scheduling step.
     */
    public void pollingJobUpdate(final PollingJobDetails pollingJobDetails) throws UnsupportedRequestTypeException, PollingJobSchedulingException {
        throwUnsupportedRequestTypeExceptionIfRequestTypeDoesNotFitApiType(pollingJobDetails);
        try {
            scheduler.deleteJob(supportedJobs.get(pollingJobDetails.id()));
            scheduleJob(pollingJobDetails);
        } catch (SchedulerException e) {
            Log.error("Failed to schedule polling job for API: " + pollingJobDetails.id(), e);
            throw new PollingJobSchedulingException("Failed to schedule polling job: " + e);
        }
    }

    /*@
    @ requires supportedJobs.get(id) != null
     */
    /**
     * Deletes PollingJob in supportedJobs and deactivates the JobKey if it was active.
     *
     * @param id used to find the pollingJob to delete.
     * @throws PollingJobSchedulingException if an exception was thrown during the deletion of PollingJob.
     */
    public void pollingJobDeletion(final Long id) throws PollingJobSchedulingException{
        final JobKey jobKey = supportedJobs.get(id);
        try {
            scheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            final String exceptionMessage = "Failed to delete PollingJob with the id " + id + "with the name " + jobKey.getName();
            Log.error(exceptionMessage, e);
            throw new PollingJobSchedulingException(exceptionMessage, e);
        }
    }


    /**
     * Checks if supportedJobs contains key that equals given id
     *
     * @param id compared to supportedJobs Keys
     * @return true if supportedJobs contains id as key
     */
    public boolean pollingJobExists(final Long id) {
        return supportedJobs.containsKey(id);
    }



    /**
     * Creates a PollingJob and schedules it using a created Trigger. Adds scheduled PollingJob to supportedJobs.
     *
     * @param pollingJobDetails used to create JobDetail and Trigger
     * @throws SchedulerException if scheduling was not possible.
     */
    private void scheduleJob(final PollingJobDetails pollingJobDetails) throws SchedulerException {
        final JobDetail job = createJobWithPollingJobDetails(pollingJobDetails);
        final Trigger trigger = createTriggerWithPollingJobDetails(pollingJobDetails);
        scheduler.scheduleJob(job, trigger);
        supportedJobs.put(pollingJobDetails.id(), job.getKey());
        feedbackProducer.publishPollingJobFeedback(pollingJobDetails.id(), JobDeploymentStatus.DEPLOYED);

        Log.infof("Polling for Job with id %d was activated with timing %d", pollingJobDetails.id(), pollingJobDetails.pollingIntervallTimeInMs(), job.getKey());
    }

    /**
     * Creates a PollingJob that later can be executed by the Quartz Scheduler.
     *
     * @param pollingJobDetails contain the needed data to execute the PollingJob.
     * @return JobDetail that later can be scheduled.
     */
    private JobDetail createJobWithPollingJobDetails(PollingJobDetails pollingJobDetails) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("requestConfiguration", pollingJobDetails);
        return JobBuilder.newJob(PollingJob.class)
                .withIdentity("apiPollingJob-" + pollingJobDetails.id())
                .usingJobData(dataMap)
                .build();
    }

    /**
     * Creates a Trigger for the Quartz Scheduler with the pollingIntervalTiming of the pollingJobDetails.
     *
     * @param pollingJobDetails contain the needed data to create the Trigger.
     * @return a trigger, configured with the data of the pollingJobDetails.
     */
    private Trigger createTriggerWithPollingJobDetails(PollingJobDetails pollingJobDetails) {
        return TriggerBuilder.newTrigger()
                .withIdentity("apiPollingTrigger-" + pollingJobDetails.id())
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(pollingJobDetails.pollingIntervallTimeInMs())
                        .repeatForever())
                .build();
    }

    /**
     * Checks if RequestType of requestBuildingDetails is supported by the API Type of the requestBuildingDetails.
     *
     * @param requestBuildingDetails contain the needed information
     * @return true if the Api Type supports the Request Type
     */
    private boolean isRequestTypeOnApiTypePossible(final RequestBuildingDetails requestBuildingDetails) {
        return !Objects.equals(requestBuildingDetails.sourceSystem().apiType(), "No Spec")
                || Objects.equals(requestBuildingDetails.endpoint().httpRequestType(), "GET");
    }

    /**
     * Throws UnsupportedRequestTypeException if the RequestType is not supported by the Api Type.
     *
     * @param pollingJobDetails contain requestType and apiType data that is checked.
     * @throws UnsupportedRequestTypeException if requestType does not fit apiType.
     */
    private void throwUnsupportedRequestTypeExceptionIfRequestTypeDoesNotFitApiType(final PollingJobDetails pollingJobDetails) throws UnsupportedRequestTypeException {
        if (!isRequestTypeOnApiTypePossible(pollingJobDetails.requestBuildingDetails())) {
            final String exceptionMessage = "PollingJob configuration failed for " + pollingJobDetails.name() + " with id " + pollingJobDetails.id() + ": Use of PUT or POST is not possible on No Spec API´s";
            Log.errorf(exceptionMessage);
            throw new UnsupportedRequestTypeException(exceptionMessage);
        }
    }


}