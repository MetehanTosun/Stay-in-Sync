package de.unistuttgart.stayinsync.pollingnode.execution.ressource;

import de.unistuttgart.stayinsync.pollingnode.exceptions.pollingjob.RequestExecutionException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.pollingjob.ResponseSubscriptionException;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.ExecutionException;


/**
 * Offers methods to build CRUD requests excluding DELETE, as well as the method executeRequest(HttpRequest<Buffer> request),
 * that executes these requests and returns the polled data.
 */
@ApplicationScoped
public class RestClient {

    /**
     * Polls Rest API Data with a prebuilt request and returns a JsonObject when poll is done.
     *
     * @param request is the prebuild parameterised HttpRequest that is executed
     * @return polled JsonObject containing the data of the specific endpoint polled from the SourceSystem.
     * @throws RequestExecutionException if a RuntimeException occurred in the request execution process
     * @throws ResponseSubscriptionException if poll could not be finished before next poll of the same supported SourceSystem was started by this class.
     */
    public JsonObject pollJsonObjectFromApi(final HttpRequest<Buffer> request) throws RequestExecutionException, ResponseSubscriptionException {
        try{
            return retrieveJsonObjectFromResponse(executeRequest(request));
        } catch (ExecutionException e) {
            final String exceptionMessage = "During the execution of this request a RuntimeException was thrown.";
            Log.warnf(exceptionMessage, e, request);
            throw new RequestExecutionException(exceptionMessage, e, request);
        } catch (InterruptedException e) {
            final String exceptionMessage = "The response subscription of the request was interrupted by a new response created by a new request execution.";
            Log.warnf(exceptionMessage, e, request);
            throw new ResponseSubscriptionException(exceptionMessage, e, request);
        }
    }

    /**
     * Executes request and returns Response in Uni Format
     *
     * @param request that is executed
     * @return Uni<HttpResponse<Buffer> from which the HttpResponse can be retrieved when the poll is finished.
     */
    private Uni<HttpResponse<Buffer>> executeRequest(final HttpRequest<Buffer> request) {
        return request.send()
                .onItem().transform(response -> {
                    Log.info(response.bodyAsString());
                    return response;
                });
    }

    /**
     * Subscribes to the Uni of the given response and unpacks and returns it´s JsonObject when the poll is finished
     *
     * @param response of an executed request
     * @return retrieved JsonObject
     * @throws ExecutionException if a RuntimeException was thrown during the request execution leading to the response
     * @throws InterruptedException if the response was not fully finished when a new response occurred
     */
    private JsonObject retrieveJsonObjectFromResponse(final Uni<HttpResponse<Buffer>> response) throws ExecutionException, InterruptedException {
            return response.subscribe()
                    .asCompletionStage()
                    .get()
                    .bodyAsJsonObject();
    }

}
