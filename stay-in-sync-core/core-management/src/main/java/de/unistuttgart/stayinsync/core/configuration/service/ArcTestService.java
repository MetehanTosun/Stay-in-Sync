package de.unistuttgart.stayinsync.core.configuration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystemEndpoint;
import de.unistuttgart.stayinsync.core.configuration.exception.CoreManagementException;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ArcTestRequestDTO;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.ArcTestResponseDTO;
import de.unistuttgart.stayinsync.core.configuration.util.TypeScriptTypeGenerator;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * A service dedicated to performing live test calls for API Request Configurations (ARCs).
 * <p>
 * This service acts as an HTTP client to execute a request against a target system based on the
 * parameters provided by the user in the ARC wizard. Its primary responsibilities are:
 * <ul>
 *   <li>Constructing and sending a real HTTP request using Vert.x WebClient for non-blocking I/O.</li>
 *   <li>Handling various response scenarios, including success, HTTP errors, and network failures.</li>
 *   <li>On a successful call with a JSON response, it utilizes the {@link TypeScriptTypeGenerator}
 *       to generate a TypeScript Declaration File (.d.ts) from the response payload.</li>
 * </ul>
 * The result is an asynchronous {@link Uni} that provides a detailed response DTO for the frontend.
 */
@ApplicationScoped
public class ArcTestService {

    // Constants to replace magic numbers and strings
    private static final int HTTP_SUCCESS_START = 200;
    private static final int HTTP_SUCCESS_END = 300;
    private static final int NETWORK_ERROR_STATUS_CODE = 503; // Service Unavailable
    private static final String DTS_FOR_EMPTY_BODY = "export type Root = any;";
    private static final String DTS_FOR_INVALID_JSON = "/* Could not generate type: Response was not valid JSON. */";

    @Inject
    Vertx vertx;

    private WebClient webClient;

    @Inject
    TypeScriptTypeGenerator typeGenerator;

    /**
     * Initializes the Vert.x {@link WebClient} after the service bean has been constructed.
     */
    @PostConstruct
    void init() {
        this.webClient = WebClient.create(vertx);
    }

    /**
     * Performs a live, asynchronous HTTP test call based on the provided ARC configuration.
     * This method orchestrates the process of finding entities, building the request, sending it,
     * and processing the response.
     *
     * @param requestDTO The {@link ArcTestRequestDTO} containing all necessary information for the test call.
     * @return A {@link Uni} that will asynchronously emit an {@link ArcTestResponseDTO} with the result.
     * @throws CoreManagementException if the source system or endpoint cannot be found.
     */
    public Uni<ArcTestResponseDTO> performTestCall(ArcTestRequestDTO requestDTO) {
        SourceSystem system = findSourceSystemOrThrow(requestDTO.sourceSystemId());
        SourceSystemEndpoint endpoint = findEndpointOrThrow(requestDTO.endpointId());

        String fullUrl = buildUrlWithPathParams(system.apiUrl, endpoint.endpointPath, requestDTO.pathParameters());
        Log.infof("Executing test call to: %s", fullUrl);

        var request = webClient.requestAbs(HttpMethod.valueOf(endpoint.httpRequestType.toUpperCase()), fullUrl);

        // Configure query parameters and headers
        if (requestDTO.queryParameterValues() != null) {
            requestDTO.queryParameterValues().forEach(request::addQueryParam);
        }
        if (requestDTO.headerValues() != null) {
            requestDTO.headerValues().forEach(request.headers()::add);
        }

        return request.send()
                .map(response -> {
                    int statusCode = response.statusCode();
                    if (!isSuccessStatusCode(statusCode)) {
                        return new ArcTestResponseDTO(false, statusCode, null, null, response.statusMessage());
                    }

                    String responseBody = response.bodyAsString();
                    if (responseBody == null || responseBody.isEmpty()) {
                        return new ArcTestResponseDTO(true, statusCode, null, DTS_FOR_EMPTY_BODY, null);
                    }

                    try {
                        String generatedDts = typeGenerator.generate(responseBody);
                        return new ArcTestResponseDTO(true, statusCode, response.bodyAsJson(JsonNode.class), generatedDts, null);
                    } catch (JsonProcessingException e) {
                        Log.warnf(e, "Could not generate DTS. Response may not be valid JSON.");
                        return new ArcTestResponseDTO(true, statusCode, responseBody, DTS_FOR_INVALID_JSON, "Response was not valid JSON.");
                    }
                })
                .onFailure().recoverWithItem(this::handleNetworkFailure);
    }

    /**
     * Handles network-level failures (e.g., connection refused, timeout).
     *
     * @param throwable The exception that occurred.
     * @return An {@link ArcTestResponseDTO} representing the network failure.
     */
    private ArcTestResponseDTO handleNetworkFailure(Throwable throwable) {
        return new ArcTestResponseDTO(false, NETWORK_ERROR_STATUS_CODE, null, null, "Network Error: " + throwable.getMessage());
    }

    /**
     * Finds a SourceSystem by its ID or throws a NOT_FOUND exception.
     */
    private SourceSystem findSourceSystemOrThrow(Long systemId) {
        return SourceSystem.<SourceSystem>findByIdOptional(systemId)
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Source system not found", "Source system with ID %s not found", systemId));
    }

    /**
     * Finds a SourceSystemEndpoint by its ID or throws a NOT_FOUND exception.
     */
    private SourceSystemEndpoint findEndpointOrThrow(Long endpointId) {
        return SourceSystemEndpoint.<SourceSystemEndpoint>findByIdOptional(endpointId)
                .orElseThrow(() -> new CoreManagementException(Response.Status.NOT_FOUND, "Endpoint not found", "Endpoint with ID %s not found", endpointId));
    }

    /**
     * Constructs the full request URL by replacing path parameter placeholders.
     */
    private String buildUrlWithPathParams(String baseUrl, String path, Map<String, String> pathParameters) {
        if (pathParameters != null) {
            for (Map.Entry<String, String> entry : pathParameters.entrySet()) {
                path = path.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return baseUrl + path;
    }

    /**
     * Checks if an HTTP status code is in the 2xx success range.
     */
    private boolean isSuccessStatusCode(int statusCode) {
        return statusCode >= HTTP_SUCCESS_START && statusCode < HTTP_SUCCESS_END;
    }
}