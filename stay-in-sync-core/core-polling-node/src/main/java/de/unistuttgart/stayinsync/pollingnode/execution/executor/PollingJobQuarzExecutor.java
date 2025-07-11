package de.unistuttgart.stayinsync.pollingnode.execution.executor;

import de.unistuttgart.stayinsync.pollingnode.exceptions.FaultySourceSystemApiRequestMessageDtoException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.JsonObjectUnthreadingException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingNodeException;
import de.unistuttgart.stayinsync.pollingnode.execution.ressource.RestClient;
import de.unistuttgart.stayinsync.pollingnode.rabbitmq.SyncDataProducer;
import de.unistuttgart.stayinsync.transport.dto.ApiConnectionDetailsDTO;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SyncDataMessageDTO;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class PollingJobQuarzExecutor implements Job {

    final ApiDetailsContainerForExecutor apiDetailsContainerForExecutor;
    final RestClient restClient;
    final SyncDataProducer syncDataProducer;

    public PollingJobQuarzExecutor(final RestClient restClient, final ApiDetailsContainerForExecutor apiDetailsContainerForExecutor, final SyncDataProducer syncDataProducer){
        this.apiDetailsContainerForExecutor = apiDetailsContainerForExecutor;
        this.restClient = restClient;
        this.syncDataProducer = syncDataProducer;
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        long configId = dataMap.getLong("configId");

        SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage =
                apiDetailsContainerForExecutor.getDetailsForSpecificJob(configId);

        if (apiRequestConfigurationMessage == null) {
            Log.errorf("No configuration found for configId: %d", configId);
            throw new JobExecutionException("Configuration not found for ID: " + configId);
        }

        try {
            JsonObject jsonObject = unpackUniJsonObject(
                    pollUniJsonObject(apiRequestConfigurationMessage.apiConnectionDetails(), restClient)
            );
            Log.infof("JsonObject polled successfully for configId %d: %s", configId, jsonObject);
            syncDataProducer.setupRequestConfigurationStream(apiRequestConfigurationMessage);
            syncDataProducer.publishSyncData(this.convertJsonObjectToSyncDataMessageDTO(apiRequestConfigurationMessage, jsonObject));
        } catch (FaultySourceSystemApiRequestMessageDtoException | PollingNodeException e) {
            Log.errorf("Error during polling execution for configId %d", configId, e);
            throw new JobExecutionException(e);
        }
    }

    /**
     * Configures a request and polls it afterward
     * @param connectionDetails
     * @param restClient
     * @return
     * @throws FaultySourceSystemApiRequestMessageDtoException
     */
    private Uni<JsonObject> pollUniJsonObject(final ApiConnectionDetailsDTO connectionDetails, final RestClient restClient) throws FaultySourceSystemApiRequestMessageDtoException {
        return restClient.executeRequest(restClient.configureRequest(connectionDetails));
    }

    /**
     * Unpacks Uni<JsonObect> and returns normal JsonObject
     * @param uniJsonObject
     * @return
     * @throws JsonObjectUnthreadingException
     */
    private JsonObject unpackUniJsonObject(final Uni<JsonObject> uniJsonObject) throws JsonObjectUnthreadingException {
        try {
            return uniJsonObject
                    .subscribe().asCompletionStage()
                    .toCompletableFuture()
                    .get();
        } catch (InterruptedException e) {
            throw new JsonObjectUnthreadingException("The unpacking of CompletableFuture to obtain JsonObject was interrupted with this exception message: " + e);
        } catch (ExecutionException e) {
            throw new JsonObjectUnthreadingException("The unpacking of CompletableFuture to obtain JsonObject was interrupted by an unforseen exception in the api call process: " + e);
        }
    }

    private SyncDataMessageDTO convertJsonObjectToSyncDataMessageDTO(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfiguration, final JsonObject jsonObject){
        Map<String,Object> map = new HashMap<>();
        map.put(apiRequestConfiguration.name(), jsonObject.getMap());
        return new SyncDataMessageDTO(apiRequestConfiguration.id(), map);
    }
}