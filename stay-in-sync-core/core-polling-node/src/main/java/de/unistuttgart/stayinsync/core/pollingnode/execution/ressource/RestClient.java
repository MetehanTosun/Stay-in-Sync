package de.unistuttgart.stayinsync.core.pollingnode.execution.ressource;

import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.restclientexceptions.RequestExecutionException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.restclientexceptions.ResponseInvalidFormatException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.restclientexceptions.ResponseSubscriptionException;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.ExecutionException;


/**
 * Offers method executeRequest to execute prebuild requests and to return the retrieved JsonObject.
 */
@ApplicationScoped
public class RestClient {

    /**
     * Polls Rest API Data with a prebuilt request and returns a JsonObject when poll is done.
     *
     * @param request is the prebuild parameterised HttpRequest that is executed
     * @return polled JsonObject containing the data of the specific endpoint polled from the SourceSystem.
     * @throws RequestExecutionException     if a RuntimeException occurred in the request execution process
     * @throws ResponseSubscriptionException if poll could not be finished before next poll of the same supported SourceSystem was started by this class.
     */
    public JsonObject pollJsonObjectFromApi(final HttpRequest<Buffer> request) throws RequestExecutionException, ResponseSubscriptionException {
        try {
            return this.retrieveJsonObjectFromUniResponse(this.executeRequest(request));
        } catch (ExecutionException e) {
            final String exceptionMessage = "During the execution of this request a RuntimeException was thrown in form of an ExecutionException.";
            Log.errorf(exceptionMessage, e);
            throw new RequestExecutionException(exceptionMessage, e);
        } catch (InterruptedException e) {
            final String exceptionMessage = "The response subscription of the request was interrupted by a new response created by a new request execution.";
            Log.errorf(exceptionMessage, e);
            throw new ResponseSubscriptionException(exceptionMessage, e);
        } catch (ResponseInvalidFormatException e) {
            Log.errorf(e.getMessage(), e);
            throw new ResponseSubscriptionException(e.getMessage(), e);
        }
    }

    /**
     * Executes request and returns Response in Uni Format
     *
     * @param request that is executed
     * @return Uni<HttpResponse < Buffer> from which the HttpResponse can be retrieved when the poll is finished.
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
     * @param responseUniFormat of an executed request
     * @return retrieved JsonObject
     * @throws ExecutionException   if a RuntimeException was thrown during the request execution leading to the response
     * @throws InterruptedException if the response was not fully finished when a new response occurred
     * @throws ResponseInvalidFormatException if the response did not contain a Json but data of a different type.
     */
    private JsonObject retrieveJsonObjectFromUniResponse(final Uni<HttpResponse<Buffer>> responseUniFormat) throws ExecutionException, InterruptedException, ResponseInvalidFormatException {
        final HttpResponse<Buffer> response = extractResponseFromUniContainer(responseUniFormat);
        return extractJsonObjectFromResponse(response);
    }

    /**
     * Subscribes to the Uni-Container to extract the response wrapped inside of it.
     * @param responseUniFormat the Uni-Container containing the response.
     * @return the extracted response.
     * @throws InterruptedException if another value was received before the unpacking was finished.
     * @throws ExecutionException if a RuntimeException was thrown during the request execution leading to the response.
     */
    private HttpResponse<Buffer> extractResponseFromUniContainer(Uni<HttpResponse<Buffer>> responseUniFormat) throws InterruptedException, ExecutionException {
        return responseUniFormat.subscribe()
                .asCompletionStage()
                .get();
    }

    /**
     * Tries to extract the JsonObject of the response. If the response does not contain an object, but an array as the outer entity,
     * the array is converted to a JsonObject containing it in the field 'entities' and then returned.
     * @param response the response of which teh JsonObject needs to be extracted.
     * @return extracted JsonObject
     * @throws ResponseInvalidFormatException if the body of the response was in a format incompatible to JsonObjects.
     */
    private JsonObject extractJsonObjectFromResponse(HttpResponse<Buffer> response) throws ResponseInvalidFormatException{
        try{
            return response.bodyAsJsonObject();
        } catch(DecodeException e){
            try {
                return new JsonObject().put("entities", response.bodyAsJsonArray());
            } catch (DecodeException e2) {
                throw new ResponseInvalidFormatException("The response was not in Json-Format and therefore could not be converted into a JsonObject", e2);
            }
        }
    }
}




