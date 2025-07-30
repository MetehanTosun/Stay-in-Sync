package de.unistuttgart.stayinsync.pollingnode.execution.pollingjob;

import de.unistuttgart.stayinsync.pollingnode.entities.PollingJobDetails;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.requestbuilderexceptions.RequestBuildingException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.restclientexceptions.RequestExecutionException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.restclientexceptions.ResponseSubscriptionException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.rabbitmqexceptions.ProducerPublishDataException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.rabbitmqexceptions.ProducerSetUpStreamException;
import de.unistuttgart.stayinsync.pollingnode.execution.ressource.RequestBuilder;
import de.unistuttgart.stayinsync.pollingnode.execution.ressource.RestClient;
import de.unistuttgart.stayinsync.pollingnode.rabbitmq.SyncDataProducer;
import de.unistuttgart.stayinsync.transport.dto.SyncDataMessageDTO;
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

    public PollingJob() {
        restClient = CDI.current().select(RestClient.class).get();
        syncDataProducer = CDI.current().select(SyncDataProducer.class).get();
    }

    /**
     * Main method of a PollingJob that can be executed by a scheduler.
     * Configures a request with the requestBuilder, executes it with the restVlient and
     * @param context
     * @throws JobExecutionException
     */
    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        PollingJobDetails pollingJobDetails = (PollingJobDetails) dataMap.get("requestConfiguration");

        if (pollingJobDetails == null) {
            Log.errorf("No configuration found for configId: %d", pollingJobDetails.id());
            throw new JobExecutionException("Configuration not found for ID: " + pollingJobDetails.id());
        }
        try {
           final JsonObject jsonObject = restClient.pollJsonObjectFromApi(requestBuilder.buildRequest(pollingJobDetails.requestBuildingDetails()));
           syncDataProducer.setupRequestConfigurationStream(pollingJobDetails);
           syncDataProducer.publishSyncData(this.convertJsonObjectToSyncDataMessageDTO(pollingJobDetails, jsonObject));

        } catch (RequestBuildingException | RequestExecutionException | ResponseSubscriptionException | ProducerSetUpStreamException | ProducerPublishDataException e) {
            final String exceptionMessage = "Error during polling execution for configId " + pollingJobDetails.id() +". " + e.getMessage();
            Log.errorf(exceptionMessage, e);
            throw new JobExecutionException(exceptionMessage, e);
        }
    }

    private SyncDataMessageDTO convertJsonObjectToSyncDataMessageDTO(final PollingJobDetails pollingJobDetails, final JsonObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        map.put(pollingJobDetails.name(), jsonObject.getMap());
        return new SyncDataMessageDTO(pollingJobDetails.id(), map);
    }
}