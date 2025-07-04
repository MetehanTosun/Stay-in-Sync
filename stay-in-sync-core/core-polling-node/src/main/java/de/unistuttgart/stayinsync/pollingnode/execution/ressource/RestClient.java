package de.unistuttgart.stayinsync.pollingnode.execution.ressource;

import de.unistuttgart.stayinsync.pollingnode.entities.ApiConnectionDetails;
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
import java.util.Objects;
import java.util.Optional;

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
     *
     * @param connectionDetails
     * @return
     */
    public Optional<HttpRequest<Buffer>> configureGetRequest(final ApiConnectionDetails connectionDetails) {
        if (connectionDetails.allFieldsInCorrectFormat() && Objects.equals(connectionDetails.endpoint().httpRequestType(), "GET")) {
            final String apiCallPath = concatPaths(connectionDetails.sourceSystem().apiUrl(), connectionDetails.endpoint().endpointPath());
            this.logSourceSystemDetails(connectionDetails.sourceSystem(), apiCallPath);
            final HttpRequest<Buffer> request = webClient.getAbs(apiCallPath);
            this.applyRequestConfiguration(connectionDetails, request);
            Log.debugf("GET request successfully built", request);
            return Optional.of(request);
        } else {
            Log.warnf("The connectionDetails that were obtained from the SourceSystemApiRequestConfiguration had some null fields or apiURL or endpointPath were empty.", connectionDetails);
            return Optional.empty();
        }

    }

    public Optional<HttpRequest<Buffer>> configurePostRequest(final ApiConnectionDetails connectionDetails) {
        if (connectionDetails.allFieldsInCorrectFormat() && Objects.equals(connectionDetails.endpoint().httpRequestType(), "POST")) {
            final String apiCallPath = concatPaths(connectionDetails.sourceSystem().apiUrl(), connectionDetails.endpoint().endpointPath());
            this.logSourceSystemDetails(connectionDetails.sourceSystem(), apiCallPath);
            final HttpRequest<Buffer> request = webClient.postAbs(apiCallPath);
            this.applyRequestConfiguration(connectionDetails, request);
            Log.debugf("POST request successfully built", request);
            return Optional.of(request);
        } else {
            Log.warnf("The connectionDetails that were obtained from the SourceSystemApiRequestConfiguration had some null fields or apiURL or endpointPath were empty.\"", connectionDetails);
            return Optional.empty();
        }
    }

    public Optional<HttpRequest<Buffer>> configurePutRequest(final ApiConnectionDetails connectionDetails) {
        if (connectionDetails.allFieldsInCorrectFormat() && Objects.equals(connectionDetails.endpoint().httpRequestType(), "PUT")) {
            final String apiCallPath = concatPaths(connectionDetails.sourceSystem().apiUrl(), connectionDetails.endpoint().endpointPath());
            this.logSourceSystemDetails(connectionDetails.sourceSystem(), apiCallPath);
            final HttpRequest<Buffer> request = webClient.putAbs(apiCallPath);
            this.applyRequestConfiguration(connectionDetails, request);
            Log.debugf("PUT request successfully built", request);
            return Optional.of(request);
        } else {
            Log.warnf("The connectionDetails that were obtained from the SourceSystemApiRequestConfiguration had some null fields or apiURL or endpointPath were empty.\"", connectionDetails);
            return Optional.empty();
        }
    }

    public Uni<JsonObject> executeRequest(final HttpRequest<Buffer> request) {
        return request.send()
                .onItem().transform(Unchecked.function(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() <= 299) {
                        JsonObject jsonData = response.bodyAsJsonObject();
                        if (jsonData == null) {
                            throw new RuntimeException("Empty response for this request");
                        }
                        return jsonData;
                    } else {
                        throw new RuntimeException(String.format(
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

    private void applyRequestConfiguration(ApiConnectionDetails connectionDetails, HttpRequest<Buffer> request) {
        request.putHeader(connectionDetails.sourceSystem().authDetails().headerName(), connectionDetails.sourceSystem().authDetails().apiKey());
        connectionDetails.requestHeader().forEach(header -> request.putHeader(header.headerName(), header.headerValue()));
        connectionDetails.requestParameters().forEach(parameter -> request.addQueryParam(parameter.paramName(), parameter.paramValue()));
    }

    private String concatPaths(final String baseUrl, final String endpointPath) {
        final String cleanedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        final String cleanedEndpoint = endpointPath.startsWith("/") ? endpointPath : "/" + endpointPath;
        return cleanedBase + cleanedEndpoint;
    }

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
