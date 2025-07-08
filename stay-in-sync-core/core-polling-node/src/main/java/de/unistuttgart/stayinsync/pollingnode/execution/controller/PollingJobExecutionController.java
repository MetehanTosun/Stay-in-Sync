package de.unistuttgart.stayinsync.pollingnode.execution.controller;

import de.unistuttgart.stayinsync.polling.exception.PollingNodeException;
import de.unistuttgart.stayinsync.pollingnode.entities.ApiConnectionDetails;
import de.unistuttgart.stayinsync.pollingnode.entities.PollingJob;
import de.unistuttgart.stayinsync.pollingnode.exceptions.FaultySourceSystemApiRequestMessageDtoException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.JsonObjectUnthreadingException;
import de.unistuttgart.stayinsync.pollingnode.execution.ressource.RestClient;

import de.unistuttgart.stayinsync.transport.dto.SourceSystemApiRequestConfigurationMessageDTO;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;

import de.unistuttgart.stayinsync.pollingnode.rabbitmq.ProducerSendPolledData;

import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

@ApplicationScoped
public class PollingJobExecutionController {

    private final ProducerSendPolledData producerSendPolledData;
    private final RestClient restClient;
    private final ScheduledExecutorService jobExecutor;
    private final Map<Long, ScheduledFuture<?>> openPollingJobThreads;

    public PollingJobExecutionController( final ProducerSendPolledData producerSendPolledData, final RestClient restClient){
        super();
        this.producerSendPolledData = producerSendPolledData;
        this.restClient = restClient;
        this.openPollingJobThreads= new HashMap<>();
        this. jobExecutor = Executors.newScheduledThreadPool(10);

    }

    public void startPollingJobExecution(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage) throws FaultySourceSystemApiRequestMessageDtoException{
        timedPollingJobThreadActivation(apiRequestConfigurationMessage);
    }

    public void stopPollingJobExecution(final Long id){
        timedPollingJobThreadDeletion(id);
        openPollingJobThreads.remove(id);
    }

    public boolean pollingJobExists(final Long id){
        return openPollingJobThreads.containsKey(id);
    }

    private void timedPollingJobThreadActivation(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage) throws FaultySourceSystemApiRequestMessageDtoException {
        if(Objects.equals(apiRequestConfigurationMessage.sourceSystem().apiType(), "No Spec") && !Objects.equals(apiRequestConfigurationMessage.endpoint().httpRequestType(), "GET")){
            throw new FaultySourceSystemApiRequestMessageDtoException("Use of PUT or POST is not possible on No Spec API´s");
        }
        Runnable task = () -> {
            try {
                JsonObject jsonObject = unpackUniJsonObject(pollUniJsonObject(apiRequestConfigurationMessage));
                Log.infof("JsonObject polled successfully: %s", jsonObject);
                //TODO JsonObject wird für noch nichts genutzt
            }catch(FaultySourceSystemApiRequestMessageDtoException | JsonObjectUnthreadingException e){
                Log.error(e);
            }
        };

        ScheduledFuture<?> future = jobExecutor.scheduleAtFixedRate(
                task,
                0,
                apiRequestConfigurationMessage.pollingIntervallTimeInMs(),
                TimeUnit.MILLISECONDS
        );

        openPollingJobThreads.put(apiRequestConfigurationMessage.id(), future);
    }

    private void timedPollingJobThreadDeletion(final Long id) {
        ScheduledFuture<?> future = openPollingJobThreads.remove(id);
        if (future != null) {
            future.cancel(true);
        }
    }

    private Uni<JsonObject> pollUniJsonObject(final SourceSystemApiRequestConfigurationMessageDTO apiRequestConfigurationMessage) throws FaultySourceSystemApiRequestMessageDtoException {
        return restClient.executeRequest(restClient.configureRequest(apiRequestConfigurationMessage));
    }

    private JsonObject unpackUniJsonObject(final Uni<JsonObject> uniJsonObject) throws JsonObjectUnthreadingException {
        try{
            return uniJsonObject
                    .subscribe().asCompletionStage()
                    .toCompletableFuture()
                    .get();
        } catch(InterruptedException e){
            throw new JsonObjectUnthreadingException("The unpacking of CompletableFuture to obtain JsonObject was interrupted with this exception message: " + e);
        } catch(ExecutionException e){
            throw new JsonObjectUnthreadingException("The unpacking of CompletableFuture to obtain JsonObject was interrupted by an unforseen exception in the api call process: " + e);
        }
    }


}
