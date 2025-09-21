package de.unistuttgart.stayinsync.core.configuration.edc.service.edcconnector;

import de.unistuttgart.stayinsync.core.configuration.edc.entities.EDCInstance;
import de.unistuttgart.stayinsync.core.configuration.edc.exception.RequestBuildingException;
import io.quarkus.logging.Log;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.time.Duration;

/**
 * Offers the method configureRequest in which a request is built for later use.
 */
@ApplicationScoped
public class EDCRequestBuilder {

    private final WebClient webClient;


    public EDCRequestBuilder(final Vertx vertx) {
        this.webClient = WebClient.create(vertx, new WebClientOptions()
                .setConnectTimeout((int) Duration.ofSeconds(10).toMillis())
                .setIdleTimeout((int) Duration.ofSeconds(30).toMillis())
                .setKeepAlive(true)
                .setMaxPoolSize(100));
    }

    /**
     * Returns a configured HttpRequest for the ApiConnectionDetails.
     *
     * @params with information needed for Building process.
     * @return fully configured and parameterised HttpRequest
     * @throws RequestBuildingException if request was not built, because of faulty requestBuildingDetails
     */
    public HttpRequest<Buffer> buildRequest(final EDCInstance edcInstance, final String requestType, final String endpoint) throws RequestBuildingException {
        try {
            final HttpRequest<Buffer> request = this.buildRequestWithSpecificRequestType(requestType, edcInstance.getControlPlaneManagementUrl() + endpoint);
            this.addApiKeyAuthentication(request, edcInstance.getApiKey());
            return request;
        } catch (Exception e) {
            final String exceptionMessage = "Request Not Built!";
            Log.errorf(exceptionMessage,e.getMessage());
            throw new RequestBuildingException(exceptionMessage, e);
        }
    }

    /**
     * Builds request with one of these types: GET, POST, PUT, DELETE.
     *
     * @param httpRequestType is used to determine the requestType for the build
     * @param apiCallPath     Used to build the request
     * @return built request
     * @throws RequestBuildingException if the httpRequestType was in wrong format.
     */
    private HttpRequest<Buffer> buildRequestWithSpecificRequestType(final String httpRequestType, String apiCallPath) throws RequestBuildingException {
        try {
            URI uri = new URI(apiCallPath);
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();

            if (port == -1) {
                port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }

            return webClient.request(
                    io.vertx.core.http.HttpMethod.valueOf(httpRequestType.toUpperCase()),
                    port,
                    host,
                    path
            ).ssl(true);

        } catch (java.net.URISyntaxException e) {

            throw new RequestBuildingException("The generated apiCallPath is not a valid URI: " + apiCallPath, e);
        } catch (IllegalArgumentException e) {
            throw new RequestBuildingException("The apiRequestType '" + httpRequestType + "' is not a valid HTTP method.", e);
        }
    }


    /**
     * Adds Api-Key Authentication to Request
     *
     * @param request the prebuild http request
     * @param apiKey for x-api-key header
     */
    private void addApiKeyAuthentication(final HttpRequest<Buffer> request, final String apiKey) {
        // API Key Authentication
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            request.putHeader("X-API-Key", apiKey.trim());
        }

        // Standard Headers für EDC Management API
        request.putHeader("Accept", "application/json");
        request.putHeader("Content-Type", "application/json");
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
