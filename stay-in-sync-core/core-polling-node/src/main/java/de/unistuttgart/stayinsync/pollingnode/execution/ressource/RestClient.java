package de.unistuttgart.stayinsync.pollingnode.execution.ressource;

import de.unistuttgart.stayinsync.pollingnode.HttpRequestExecutionException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.FaultySourceSystemApiRequestMessageDtoException;
import de.unistuttgart.stayinsync.transport.dto.ApiConnectionDetailsDTO;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemMessageDTO;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.logging.Log;

import java.time.Duration;

/**
 * Offers methods to build CRUD requests excluding DELETE, as well as the method executeRequest(HttpRequest<Buffer> request),
 * that executes these requests and returns the polled data.
 */
@ApplicationScoped
public class RestClient {

    private final WebClient webClient;

    /**
     * Constructs RestClient with single WebClient to increase polling efficiency by removing that step from request execution process.
     * With the constant in setMaxPoolSize you can define how many threads can be operated simultaneously and therefore are
     * able to define the vertical scalability of this polling node.
     *
     * @param vertx used to manage the WebClient's event loop, connection pooling, and thread model.
     *              Must be provided by the Quarkus dependency injection to ensure proper lifecycle management.
     */
    //TODO Add option to decide vertical scalability of polling node at deployment
    public RestClient(final Vertx vertx) {
        this.webClient = WebClient.create(vertx, new WebClientOptions()
                .setConnectTimeout((int) Duration.ofSeconds(10).toMillis())
                .setIdleTimeout((int) Duration.ofSeconds(30).toMillis())
                .setKeepAlive(true)
                .setMaxPoolSize(100));

    }

    /**
     * Returns a configured HttpRequest for the ApiConnectionDetails.
     * @param connectionDetails contains important info for request build.
     * @return fully configured and parameterised HttpRequest
     * @throws FaultySourceSystemApiRequestMessageDtoException if configuration didn´t work because of some fields.
     */
    public HttpRequest<Buffer> configureRequest(final ApiConnectionDetailsDTO connectionDetails) throws FaultySourceSystemApiRequestMessageDtoException {
        try {
            final String apiCallPath = concatPaths(connectionDetails.sourceSystem().apiUrl(), connectionDetails.endpoint().endpointPath());
            this.logSourceSystemDetails(connectionDetails.sourceSystem(), apiCallPath);
            HttpRequest<Buffer> request = buildRequestWithSpecificRequestType(connectionDetails.endpoint().httpRequestType(), apiCallPath);
            this.applyRequestConfiguration(connectionDetails, request);
            Log.debugf("Request successfully built", request);
            return request;
        } catch (Exception e) {
            throw new FaultySourceSystemApiRequestMessageDtoException("Request configuration failed for " + connectionDetails.sourceSystem().toString());
        }
    }

    /**
     * Executes a prebuilt request and returns a JsonObject when poll is done.
     *
     * @param request is the prebuild parameterised HttpRequest that is executed
     * @return Uni<JsonObject> form which the JsonObject can be retrieved
     */
    public Uni<JsonObject> executeRequest(final HttpRequest<Buffer> request) {
        return request.send()
                .onItem().transform(Unchecked.function(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() <= 299) {
                        JsonObject jsonData = response.bodyAsJsonObject();
                        if (jsonData == null) {
                            throw new HttpRequestExecutionException("Empty response for this request");
                        }
                        return jsonData;
                    } else {
                        throw new HttpRequestExecutionException(String.format(
                                "Http error %d with response status message %s for this request",
                                response.statusCode(), response.statusMessage()));
                    }
                }))
                .onItem().invoke(() ->
                        Log.debugf("Json polled successfully")
                )
                .onFailure().invoke(throwable ->
                        Log.errorf(throwable, throwable.getMessage(), request)
                );
    }

    /**
     * Builds request with one of these types: GET, POST, PUT.
     * DELETE is not supported.
     *
     * @param httpRequestType is used to determine the requestType for the build
     * @param apiCallPath Used to build the request
     * @return built request
     * @throws FaultySourceSystemApiRequestMessageDtoException if the httpRequestType was in wrong format.
     */
    private HttpRequest<Buffer> buildRequestWithSpecificRequestType(final String httpRequestType, String apiCallPath) throws FaultySourceSystemApiRequestMessageDtoException {
        HttpRequest<Buffer> request;
        switch (httpRequestType) {
            case "GET" -> request = webClient.getAbs(apiCallPath);
            case "POST" -> request = webClient.postAbs(apiCallPath);
            case "PUT" -> request = webClient.putAbs(apiCallPath);
            default ->
                    throw new FaultySourceSystemApiRequestMessageDtoException("The apiRequestType was not 'GET', 'POST', 'PUT'");
        }
        return request;
    }

    /*
     * Parameterises an already built HttpRequest with the information of the ApiConnectionDetails.
     */
    private void applyRequestConfiguration(ApiConnectionDetailsDTO connectionDetails, HttpRequest<Buffer> request) {
        request.putHeader(connectionDetails.sourceSystem().authDetails().headerName(), connectionDetails.sourceSystem().authDetails().apiKey());
        connectionDetails.requestHeader().forEach(header -> request.putHeader(header.headerName(), header.headerValue()));
        connectionDetails.requestParameters().forEach(parameter -> {
            switch (parameter.type()) {
                //TODO PathParam richtig einführen
                case PATH -> request.addPathParam(parameter.paramName(), parameter.paramValue());
                case QUERY -> request.addQueryParam(parameter.paramName(), parameter.paramValue());
                default -> throw new IllegalArgumentException("Unsupported parameter type: " + parameter.type());
            }
        });
    }

    /*
     * Applies second to first path. Changes all "\" to "/" and makes sure, that there are no additional or missing "/" between the seems.
     */
    private String concatPaths(final String baseUrl, final String endpointPath) throws FaultySourceSystemApiRequestMessageDtoException {
        if (baseUrl == null || endpointPath == null) {
            throw new IllegalArgumentException("BaseURL and endpointPath must not be null");
        }
        String base = baseUrl.replace("\\", "/").replaceAll("/+$", "");
        String endpoint = endpointPath.replace("\\", "/").replaceAll("^/+", "");
        if (base.isEmpty()) {
            return endpoint;
        }
        if (endpoint.isEmpty()) {
            return base;
        }
        return base + "/" + endpoint;
    }

    /*
     * Logs SourceSystem Details for debugging.
     */
    private void logSourceSystemDetails(final SourceSystemMessageDTO sourceSystem, final String apiCallPath) {
        Log.debugf("""
                GET called on SourceSystem with these details:
                name: %s
                apiType: %s
                apiPath: %s
                """, sourceSystem.name(), sourceSystem.apiType(), apiCallPath);
    }

    /**
     * Shuts down the WebClient and releases associated resources.
     * This method is automatically invoked by the container during application shutdown
     * to ensure all underlying network connections and thread resources are properly cleaned up.
     */
    @jakarta.annotation.PreDestroy
    public void cleanup() {
        if (webClient != null) {
            webClient.close();
            Log.debug("WebClient was closed correctly");
        }
    }
}
