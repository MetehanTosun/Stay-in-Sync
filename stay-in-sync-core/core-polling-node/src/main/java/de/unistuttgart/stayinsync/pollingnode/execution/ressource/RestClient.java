package de.unistuttgart.stayinsync.pollingnode.execution.ressource;

import de.unistuttgart.stayinsync.pollingnode.exceptions.FaultySourceSystemApiRequestMessageDtoException;
import de.unistuttgart.stayinsync.transport.dto.ApiConnectionDetailsDTO;
import de.unistuttgart.stayinsync.transport.dto.ApiRequestParameterMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.ParamType;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemMessageDTO;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
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
     *
     * @param connectionDetails contains important info for request build.
     * @return fully configured and parameterised HttpRequest
     * @throws FaultySourceSystemApiRequestMessageDtoException if configuration didn´t work because of some fields.
     */
    public HttpRequest<Buffer> configureRequest(final ApiConnectionDetailsDTO connectionDetails) throws FaultySourceSystemApiRequestMessageDtoException {
        try {
            String resolvedPath = resolvePath(connectionDetails);
            this.logSourceSystemDetails(connectionDetails.sourceSystem(), resolvedPath);
            HttpRequest<Buffer> request = buildRequestWithSpecificRequestType(connectionDetails.endpoint().httpRequestType(), resolvedPath);
            this.applyRequestConfiguration(connectionDetails, request);
            Log.infof("Request successfully built %s", request.toString());
            return request;
        } catch (Exception e) {
            throw new FaultySourceSystemApiRequestMessageDtoException("Request configuration failed for " + connectionDetails.sourceSystem().toString(), e);
        }
    }

    /**
     * Executes a prebuilt request and returns a JsonObject when poll is done.
     *
     * @param request is the prebuild parameterised HttpRequest that is executed
     * @return Uni<JsonObject> form which the JsonObject can be retrieved
     */
    public Uni<HttpResponse<Buffer>> executeRequest(final HttpRequest<Buffer> request) {
        return request.send()
                .onItem().transform(response -> {
                    Log.info(response.bodyAsString());
                    return response;
                });
    }

    /**
     * Builds request with one of these types: GET, POST, PUT.
     * DELETE is not supported.
     *
     * @param httpRequestType is used to determine the requestType for the build
     * @param apiCallPath     Used to build the request
     * @return built request
     * @throws FaultySourceSystemApiRequestMessageDtoException if the httpRequestType was in wrong format.
     */
    private HttpRequest<Buffer> buildRequestWithSpecificRequestType(final String httpRequestType, String apiCallPath) throws FaultySourceSystemApiRequestMessageDtoException {
        try {
            URI uri = new URI(apiCallPath);
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();

            if (port == -1) {
                port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }

            // Earlier implementation with getAbs() locks down the URI so that no PathTemplates are exchangeable
            // Thus we are building the request manually with all required data present.
            return webClient.request(
                    io.vertx.core.http.HttpMethod.valueOf(httpRequestType.toUpperCase()),
                    port,
                    host,
                    path
            ).ssl(true);

        } catch (java.net.URISyntaxException e) {
            throw new FaultySourceSystemApiRequestMessageDtoException("The generated apiCallPath is not a valid URI: " + apiCallPath, e);
        } catch (IllegalArgumentException e) {
            throw new FaultySourceSystemApiRequestMessageDtoException("The apiRequestType '" + httpRequestType + "' is not a valid HTTP method.", e);
        }
    }

    /*
     * Parameterises an already built HttpRequest with the information of the ApiConnectionDetails.
     */
    private void applyRequestConfiguration(ApiConnectionDetailsDTO connectionDetails, HttpRequest<Buffer> request) {
        //request.putHeader(connectionDetails.sourceSystem().authDetails().headerName(), connectionDetails.sourceSystem().authDetails().apiKey());
        request.putHeader("Host", connectionDetails.sourceSystem().apiUrl().replaceFirst("https?://", ""));
        connectionDetails.requestHeader().forEach(header -> request.putHeader(header.headerName(), header.headerValue()));
        connectionDetails.requestParameters().forEach(parameter -> {
            if (parameter.type() == ParamType.QUERY) {
                request.addQueryParam(parameter.paramName(), parameter.paramValue());
            }
        });
        Log.infof("The path is: %s", request.toString());
    }

    /*
     * Builds a valid URI path to be given to the request Handler.
     */
    private String resolvePath(ApiConnectionDetailsDTO connectionDetails) {
        String pathTemplate = connectionDetails.endpoint().endpointPath();
        for (ApiRequestParameterMessageDTO parameter : connectionDetails.requestParameters()) {
            if (parameter.type() == ParamType.PATH) {
                pathTemplate = pathTemplate.replace("{" + parameter.paramName() + "}", parameter.paramValue());
            }
        }
        return connectionDetails.sourceSystem().apiUrl() + pathTemplate;
    }

    /*
     * Logs SourceSystem Details for debugging.
     */
    private void logSourceSystemDetails(final SourceSystemMessageDTO sourceSystem, final String apiCallPath) {
        Log.infof("""
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
