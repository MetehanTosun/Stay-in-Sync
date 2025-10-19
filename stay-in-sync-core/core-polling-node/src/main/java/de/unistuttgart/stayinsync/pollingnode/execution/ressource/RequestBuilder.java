package de.unistuttgart.stayinsync.pollingnode.execution.ressource;

import de.unistuttgart.stayinsync.transport.dto.ApiRequestParameterMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.ParamType;
import de.unistuttgart.stayinsync.pollingnode.entities.RequestBuildingDetails;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.requestbuilderexceptions.RequestBuildingDetailsNullFieldException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.requestbuilderexceptions.RequestBuildingException;
import io.quarkus.logging.Log;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.time.Duration;

import static de.unistuttgart.stayinsync.transport.dto.ParamType.QUERY;


/**
 * Offers the method configureRequest in which a request is built for later use.
 */
@ApplicationScoped
public class RequestBuilder {

    private final WebClient webClient;

    /**
     * Constructs RequestBuilder with single WebClient to increase polling efficiency by removing that step from request building process.
     * With the constant in setMaxPoolSize you can define how many threads can be operated simultaneously and therefore are
     * able to define the vertical scalability of this polling node.
     *
     * @param vertx used to manage the WebClient's event loop, connection pooling, and thread model.
     *              Must be provided by the Quarkus dependency injection to ensure proper lifecycle management.
     */
    //TODO Add option to decide vertical scalability of polling node at deployment
    public RequestBuilder(final Vertx vertx) {
        this.webClient = WebClient.create(vertx, new WebClientOptions()
                .setConnectTimeout((int) Duration.ofSeconds(10).toMillis())
                .setIdleTimeout((int) Duration.ofSeconds(30).toMillis())
                .setKeepAlive(true)
                .setMaxPoolSize(100));
    }

    /**
     * Returns a configured HttpRequest for the ApiConnectionDetails.
     *
     * @param requestBuildingDetails contains important info for request building process.
     * @return fully configured and parameterised HttpRequest
     * @throws RequestBuildingException if request was not built, because of faulty requestBuildingDetails
     */
    public HttpRequest<Buffer> buildRequest(final RequestBuildingDetails requestBuildingDetails) throws RequestBuildingException {
        try {
            this.throwExceptionIfRequestBuildingDetailsInvalid(requestBuildingDetails);
            final String apiCallPath = this.resolveExecutionPath(requestBuildingDetails);
            final HttpRequest<Buffer> request = this.buildRequestWithSpecificRequestType(requestBuildingDetails.endpoint().httpRequestType(), apiCallPath);
            this.parameterizeRequest(requestBuildingDetails, request);
            return request;
        } catch (RequestBuildingDetailsNullFieldException e) {
            final String exceptionMessage = "Request Not Built! " + e.getMessage();
            Log.errorf(exceptionMessage);
            throw new RequestBuildingException(exceptionMessage, e);
        } catch (Exception e) {
            final String exceptionMessage = "Request Not Built! An unexpected Exception was thrown in the requestBuildingProcess. RequestBuilder in Pollingcomponent should be reviewed with this Exception";
            Log.errorf(exceptionMessage);
            throw new RequestBuildingException(exceptionMessage, e);
        }
    }

     /*@
    @ requires throwExceptionIfRequestBuildingDetailsInvalid(requestBuildingDetails) called before this call.
     */

    /**
     * Builds request with one of these types: GET, POST, PUT.
     * DELETE is not supported.
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

            boolean useSsl = "https".equalsIgnoreCase(uri.getScheme());

            return webClient.request(
                    io.vertx.core.http.HttpMethod.valueOf(httpRequestType.toUpperCase()),
                    port,
                    host,
                    path
            ).ssl(useSsl);

        } catch (java.net.URISyntaxException e) {

            throw new RequestBuildingException("The generated apiCallPath is not a valid URI: " + apiCallPath, e);
        } catch (IllegalArgumentException e) {
            throw new RequestBuildingException("The apiRequestType '" + httpRequestType + "' is not a valid HTTP method.", e);
        }
    }


    /*@
    @ requires throwExceptionIfRequestBuildingDetailsInvalid(requestBuildingDetails) called before this call.
     */

    /**
     * Parameterizes the request by adding the AuthHeader, other Headers and Parameters to the request.
     *
     * @param requestBuildingDetails the buildingDetails for this specific request
     * @param request                the prebuilt but not parameterized request
     */
    private void parameterizeRequest(final RequestBuildingDetails requestBuildingDetails, final HttpRequest<Buffer> request) {
        request.putHeader("Host", requestBuildingDetails.sourceSystem().apiUrl().replaceFirst("https?://", ""));
        if (requestBuildingDetails.sourceSystem().authDetails() != null) {
            request.putHeader(requestBuildingDetails.sourceSystem().authDetails().headerName(), requestBuildingDetails.sourceSystem().authDetails().apiKey());
        }
        if (requestBuildingDetails.requestHeader() != null) {
            requestBuildingDetails.requestHeader().forEach(header -> {
                if (header.headerName() != null) {
                    request.putHeader(header.headerName(), header.headerValue());
                }
            });
        }
        if (requestBuildingDetails.requestParameters() != null) {
            requestBuildingDetails.requestParameters().forEach(parameter -> {
                if (parameter.paramName() != null && parameter.type() != null && parameter.type() == QUERY) {
                    request.addQueryParam(parameter.paramName(), parameter.paramValue());
                }
            });
        }
    }

    /*@
    @ requires throwExceptionIfRequestBuildingDetailsInvalid(requestBuildingDetails) called before this call.
     */

    /**
     * Parameterizes endpointPath with PathParameters, concats them with apiURL and returns them as String.
     *
     * @param connectionDetails contain endpointPath and requestParameters.
     * @return ApiUrl and Parameterized endpointPath as one String.
     */
    private String resolveExecutionPath(final RequestBuildingDetails connectionDetails) {
        String pathTemplate = connectionDetails.endpoint().endpointPath();
        for (ApiRequestParameterMessageDTO parameter : connectionDetails.requestParameters()) {
            if (parameter.type() == ParamType.PATH) {
                pathTemplate = pathTemplate.replace("{" + parameter.paramName() + "}", parameter.paramValue());
            }
        }
        return connectionDetails.sourceSystem().apiUrl() + pathTemplate;
    }


    /*@
    @ requires throwExceptionIfRequestBuildingDetailsInvalid(requestBuildingDetails) called before this call.
    @ ensures apiUrl != null && httpRequestType != null && (apiKey && authHeaderName) != null or == null
     */

    /**
     * Throws Exception if the requestBuildingDetails would lead to an invalid request.
     *
     * @param requestBuildingDetails the details that are checked for null fields.
     * @throws RequestBuildingDetailsNullFieldException if an important field is null.
     */
    private void throwExceptionIfRequestBuildingDetailsInvalid(final RequestBuildingDetails requestBuildingDetails) throws RequestBuildingDetailsNullFieldException {
        if (requestBuildingDetails.sourceSystem() == null || requestBuildingDetails.endpoint() == null) {
            final String exceptionMessage = "No SourceSystem, Endpoint were defined in RequestBuildingDetails";
            Log.errorf(exceptionMessage);
            throw new RequestBuildingDetailsNullFieldException(exceptionMessage);
        }
        if (requestBuildingDetails.sourceSystem().apiUrl() == null) {
            final String exceptionMessage = "ApiUrl was null";
            Log.errorf(exceptionMessage, requestBuildingDetails.sourceSystem().name());
            throw new RequestBuildingDetailsNullFieldException(exceptionMessage, requestBuildingDetails.sourceSystem().name());
        }
        if (requestBuildingDetails.endpoint().httpRequestType() == null) {
            final String exceptionMessage = "HttpRequestType was null.";
            Log.errorf(exceptionMessage, requestBuildingDetails.endpoint());
            throw new RequestBuildingDetailsNullFieldException(exceptionMessage, requestBuildingDetails.sourceSystem().name());
        }
//        final String apiKey = requestBuildingDetails.sourceSystem().authDetails().apiKey();
//        final String authHeaderName = requestBuildingDetails.sourceSystem().authDetails().headerName();
//        if ((apiKey == null && authHeaderName != null) || (apiKey != null && authHeaderName == null)) {
//            final String exceptionMessage = "ApiKey and AuthHeaderName either both need to be == null or != null";
//            Log.errorf(exceptionMessage, requestBuildingDetails.sourceSystem().name());
//            throw new RequestBuildingDetailsNullFieldException(exceptionMessage, requestBuildingDetails.sourceSystem().name());
//        }
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
