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
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.quartz.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@DisallowConcurrentExecution
public class PollingJob implements Job {

    @Inject
    RestClient restClient;

    @Inject
    SyncDataProducer syncDataProducer;

    public PollingJob() {
        restClient = CDI.current().select(RestClient.class).get();
        syncDataProducer = CDI.current().select(SyncDataProducer.class).get();
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage = (SourceSystemApiRequestConfigurationMessageDTO) dataMap.get("requestConfiguration");

        // TODO: Evaluate another way to give out this log
        if (apiRequestConfigurationMessage == null) {
            Log.errorf("No configuration found for configId: %d", apiRequestConfigurationMessage.id());
            throw new JobExecutionException("Configuration not found for ID: " + apiRequestConfigurationMessage.id());
        }

        try {
            Uni<HttpResponse<Buffer>> httpResponseUni = pollUniJsonObject(apiRequestConfigurationMessage.apiConnectionDetails(), restClient);
            HttpResponse<Buffer> bufferHttpResponse = httpResponseUni.subscribe()
                    .asCompletionStage()
                    .get();
            Log.infof("Received response body: \n %s", bufferHttpResponse.bodyAsString());
            JsonObject jsonObject = bufferHttpResponse.bodyAsJsonObject();
            Log.infof("JsonObject polled successfully for configId %d: %s", apiRequestConfigurationMessage.id(), jsonObject);
            syncDataProducer.setupRequestConfigurationStream(apiRequestConfigurationMessage);
            syncDataProducer.publishSyncData(this.convertJsonObjectToSyncDataMessageDTO(apiRequestConfigurationMessage, jsonObject));
        } catch (FaultySourceSystemApiRequestMessageDtoException | PollingNodeException e) {
            Log.errorf("Error during polling execution for configId %d", apiRequestConfigurationMessage.id(), e);
            throw new JobExecutionException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Configures a request and polls it afterward
     *
     * @param connectionDetails
     * @param restClient
     * @return
     * @throws FaultySourceSystemApiRequestMessageDtoException
     */
    private Uni<HttpResponse<Buffer>> pollUniJsonObject(final ApiConnectionDetailsDTO connectionDetails, final RestClient restClient) throws FaultySourceSystemApiRequestMessageDtoException {
        HttpRequest<Buffer> bufferHttpRequest = restClient.configureRequest(connectionDetails);
        Log.infof("request headers %s", bufferHttpRequest);
        return restClient.executeRequest(bufferHttpRequest);
    }

    /**
     * Unpacks Uni<JsonObect> and returns normal JsonObject
     *
     * @param uniJsonObject
     * @return
     * @throws JsonObjectUnthreadingException
     */
    private JsonObject unpackUniJsonObject(final Uni<JsonObject> uniJsonObject) throws JsonObjectUnthreadingException {
        try {
            return uniJsonObject.onItem().transform(response -> {
                return response;
            }).onFailure().invoke(throwable -> {
                Log.error("Failed to fetch response", throwable);
                // Optional: inspect and unwrap underlying cause here
            }).subscribe().asCompletionStage().get();
        } catch (InterruptedException e) {
            throw new JsonObjectUnthreadingException("The unpacking of CompletableFuture to obtain JsonObject was interrupted with this exception message: " + e);
        } catch (ExecutionException e) {

            throw new JsonObjectUnthreadingException("The unpacking of CompletableFuture to obtain JsonObject was interrupted by an unforseen exception in the api call process: " + e);
        }
    }

    private SyncDataMessageDTO convertJsonObjectToSyncDataMessageDTO(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfiguration, final JsonObject jsonObject) {
        Map<String, Object> map = jsonObject.getMap();
        return new SyncDataMessageDTO(apiRequestConfiguration.name(), apiRequestConfiguration.id(), map);
    }
}