package de.unistuttgart.stayinsync.pollingnode.execution.ressource;

import de.unistuttgart.stayinsync.pollingnode.entities.RequestBuildingDetails;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.UnsupportedRequestTypeException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.requestbuilderexceptions.RequestBuildingDetailsNullFieldException;
import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.requestbuilderexceptions.RequestBuildingException;
import io.quarkus.logging.Log;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;

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
            final String apiCallPath = this.concatPaths(requestBuildingDetails.sourceSystem().apiUrl(), requestBuildingDetails.endpoint().endpointPath());
            final HttpRequest<Buffer> request = this.prebuildRequestWithRequestType(requestBuildingDetails.endpoint().httpRequestType(), apiCallPath);
            this.parameterizeRequest(requestBuildingDetails, request);
            return request;
        } catch (RequestBuildingDetailsNullFieldException e) {
            final String exceptionMessage = "Request Not Built! " + e.getMessage();
            Log.errorf(exceptionMessage);
            throw new RequestBuildingException(exceptionMessage, e);
        } catch (Exception e){
            final String exceptionMessage = "Request Not Built! An unexpected Exception was thrown in the requestBuildingProcess. RequestBuilder in Pollingcomponent should be reviewed with this Exception";
            Log.errorf(exceptionMessage);
            throw new RequestBuildingException(exceptionMessage, e, requestBuildingDetails);
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
     */
    private HttpRequest<Buffer> prebuildRequestWithRequestType(final String httpRequestType, String apiCallPath) {
        HttpRequest<Buffer> request;
        switch (httpRequestType) {
            case "GET" -> request = webClient.getAbs(apiCallPath);
            case "POST" -> request = webClient.postAbs(apiCallPath);
            case "PUT" -> request = webClient.putAbs(apiCallPath);
            default -> throw new UnsupportedRequestTypeException("The apiRequestType was not 'GET', 'POST', 'PUT'");
        }
        return request;
    }

    /*@
    @ requires throwExceptionIfRequestBuildingDetailsInvalid(requestBuildingDetails) called before this call.
     */
    /**
     * Parameterizes the request by adding the AuthHeader, other Headers and Parameters to the request.
     *
     * @param requestBuildingDetails the buildingDetails for this specific request
     * @param request the prebuilt but not parameterized request
     */
    private void parameterizeRequest(final RequestBuildingDetails requestBuildingDetails, final HttpRequest<Buffer> request) {
        request.putHeader("Host", requestBuildingDetails.sourceSystem().apiUrl().replaceFirst("https?://", ""));
        request.putHeader(requestBuildingDetails.sourceSystem().authDetails().headerName(), requestBuildingDetails.sourceSystem().authDetails().apiKey());
        if (requestBuildingDetails.requestHeader() != null) {
            requestBuildingDetails.requestHeader().forEach(header -> {
                if (header.headerName() != null) {
                    request.putHeader(header.headerName(), header.headerValue());
                }
            });
        }
        if(requestBuildingDetails.requestParameters() != null){
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
     * Applies second to first path. Changes all "\" to "/" and makes sure, that there are no additional or missing "/" between the seems.
     *
     * @param baseUrl      is appended before the other String.
     * @param endpointPath is appended after the other String.
     */
    private String concatPaths(String baseUrl, String endpointPath) {
        if (endpointPath == null) endpointPath = "";
        final String base = baseUrl.replace("\\", "/").replaceAll("/+$", "");
        final String endpoint = endpointPath.replace("\\", "/").replaceAll("^/+", "");
        return base + "/" + endpoint;
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
        if(requestBuildingDetails.sourceSystem() == null || requestBuildingDetails.endpoint() == null || requestBuildingDetails.sourceSystem().authDetails() == null){
            final String exceptionMessage = "No SourceSystem, Endpoint or AuthDetails were defined in RequestBuildingDetails";
            Log.errorf(exceptionMessage, requestBuildingDetails.sourceSystem());
            throw new RequestBuildingDetailsNullFieldException(exceptionMessage, requestBuildingDetails.sourceSystem());
        }
        if (requestBuildingDetails.sourceSystem().apiUrl() == null) {
            final String exceptionMessage = "ApiUrl was null";
            Log.errorf(exceptionMessage, requestBuildingDetails.sourceSystem());
            throw new RequestBuildingDetailsNullFieldException(exceptionMessage, requestBuildingDetails.sourceSystem());
        }
        if (requestBuildingDetails.endpoint().httpRequestType() == null) {
            final String exceptionMessage = "HttpRequestType was null.";
            Log.errorf(exceptionMessage, requestBuildingDetails.endpoint());
            throw new RequestBuildingDetailsNullFieldException(exceptionMessage, requestBuildingDetails.sourceSystem(), requestBuildingDetails.endpoint());
        }
        final String apiKey = requestBuildingDetails.sourceSystem().authDetails().apiKey();
        final String authHeaderName = requestBuildingDetails.sourceSystem().authDetails().headerName();
        if ((apiKey == null && authHeaderName != null) || (apiKey != null && authHeaderName == null)) {
            final String exceptionMessage = "ApiKey and AuthHeaderName either both need to be == null or != null";
            Log.errorf(exceptionMessage, requestBuildingDetails.sourceSystem().authDetails());
            throw new RequestBuildingDetailsNullFieldException(exceptionMessage, requestBuildingDetails.sourceSystem());
        }
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
