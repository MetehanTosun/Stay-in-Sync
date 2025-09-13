package de.unistuttgart.stayinsync.core.pollingnode.execution.pollingjob;

import de.unistuttgart.stayinsync.core.pollingnode.execution.ressource.RequestBuilder;
import de.unistuttgart.stayinsync.core.pollingnode.execution.ressource.RestClient;
import de.unistuttgart.stayinsync.core.pollingnode.rabbitmq.SyncDataProducer;
import de.unistuttgart.stayinsync.core.transport.dto.SyncDataMessageDTO;
import de.unistuttgart.stayinsync.pollingnode.entities.PollingJobDetails;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.requestbuilderexceptions.RequestBuildingException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.restclientexceptions.RequestExecutionException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.restclientexceptions.ResponseSubscriptionException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.rabbitmqexceptions.ProducerPublishDataException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.rabbitmqexceptions.ProducerSetUpStreamException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.logging.Log;
import io.vertx.core.json.JsonObject;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.quartz.*;

import java.util.HashMap;
import java.util.Map;

@DisallowConcurrentExecution
public class PollingJob implements Job {

    @Inject
    RestClient restClient;

    @Inject
    RequestBuilder requestBuilder;

    @Inject
    SyncDataProducer syncDataProducer;

    @Inject
    MeterRegistry registry;

    public PollingJob() {
        restClient = CDI.current().select(RestClient.class).get();
        syncDataProducer = CDI.current().select(SyncDataProducer.class).get();
        requestBuilder = CDI.current().select(RequestBuilder.class).get();
        this.registry = CDI.current().select(MeterRegistry.class).get();
    }

    /**
     * Main method of a PollingJob that can be executed by a scheduler.
     * Configures a request with the requestBuilder, executes it with the restClient and
     * @param context contains the pollingJobDetails
     * @throws JobExecutionException if no PollingJobDetails were provided or an exception occurs in the JobExecution.
     */
    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        PollingJobDetails pollingJobDetails = getPollingJobDetailsFromJobExecutionContext(context);
        if (pollingJobDetails == null) {
            final String exceptionMessage = "PollingJob was requested without providing pollingJobDetails. PollingJobExecution not aborted";
            Log.errorf(exceptionMessage);
            throw new JobExecutionException(exceptionMessage);
        }

        if (pollingJobDetails.workerPodName() != null){
            requestCounter(pollingJobDetails.workerPodName()).increment();
        }

        try {
           final JsonObject jsonObject = restClient.pollJsonObjectFromApi(requestBuilder.buildRequest(pollingJobDetails.requestBuildingDetails()));
           syncDataProducer.setupRequestConfigurationStream(pollingJobDetails);
           syncDataProducer.publishSyncData(this.convertJsonObjectToSyncDataMessageDTO(pollingJobDetails, jsonObject));

        } catch (RequestBuildingException | RequestExecutionException | ResponseSubscriptionException | ProducerSetUpStreamException | ProducerPublishDataException e) {
            final String exceptionMessage = "Error during PollingJob execution for configId " + pollingJobDetails.id() +". " + e.getMessage();
            Log.errorf(exceptionMessage, e);
            throw new JobExecutionException(exceptionMessage, e);
        }
    }

    /**
     * Extracts the pollingJobDetails from JobExecutionContext and returns them.
     * @param context contains pollingJobDetails.
     * @return pollingJobDetails extracted from context.
     */
    private PollingJobDetails getPollingJobDetailsFromJobExecutionContext(JobExecutionContext context) {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        return (PollingJobDetails) dataMap.get("requestConfiguration");
    }

    /**
     * Creates SyncDataMessageDTO out of polled pollingJob and its pollingJobDetails. The message can later be provided to the RabbitMQProducer.
     *
     * @param pollingJobDetails contain data that is included into the dataMap.
     * @param jsonObject is used to create the DataMap that is needed to create the SyncDataMessageDTO.
     * @return SyncDataMessageDTO created from the JsonObject and the pollingJobDetails.
     */
    private SyncDataMessageDTO convertJsonObjectToSyncDataMessageDTO(final PollingJobDetails pollingJobDetails, final JsonObject jsonObject) {
        return new SyncDataMessageDTO(pollingJobDetails.name(), pollingJobDetails.id(), jsonObject.getMap());
    }

   /**
    * Gibt einen Counter für die Anzahl der Polling-Requests zurück.
    * Der Counter wird mit dem Namen des Polling-Nodes als Label versehen.
    *
    * @param pollingNode Name des Polling-Nodes
    * @return Counter für die Metrik "polling_requests_total"
    */
   private Counter requestCounter(String pollingNode) {
       return registry.counter("polling_requests_total", "pollingNode", pollingNode);
   }

}