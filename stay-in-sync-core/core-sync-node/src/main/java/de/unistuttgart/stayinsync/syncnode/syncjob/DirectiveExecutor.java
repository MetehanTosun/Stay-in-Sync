package de.unistuttgart.stayinsync.syncnode.syncjob;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.exception.SyncNodeException;
import de.unistuttgart.stayinsync.syncnode.domain.ApiCallConfiguration;
import de.unistuttgart.stayinsync.syncnode.domain.UpsertDirective;
import de.unistuttgart.stayinsync.syncnode.syncjob.assets.CheckResponseCacheService;
import de.unistuttgart.stayinsync.transport.domain.TargetApiRequestConfigurationActionRole;
import de.unistuttgart.stayinsync.transport.dto.ApiRequestHeaderMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.targetsystems.ActionMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.targetsystems.RequestConfigurationMessageDTO;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.slf4j.MDC;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Executes "upsert" (update or insert) logic against target systems based on script-generated directives.
 * <p>
 * The primary purpose of this service is to implement a CHECK-then-ACT pattern for data synchronization.
 * For each {@link UpsertDirective}, it performs the following sequence:
 * <ol>
 *     <li><b>CHECK:</b> It makes an HTTP GET request to see if a resource already exists in the target system.
 *         This check is cached to avoid redundant network calls within the same transformation.</li>
 *     <li><b>ACT (CREATE or UPDATE):</b> Based on the CHECK response (404 Not Found or 200 OK), it proceeds
 *         to either create a new resource (HTTP POST) or update an existing one (HTTP PUT).</li>
 * </ol>
 * This service handles all aspects of building, executing, and logging these state-changing HTTP requests.
 */
@ApplicationScoped
public class DirectiveExecutor {

    private final CheckResponseCacheService responseCache;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the DirectiveExecutor with its required dependencies.
     *
     * @param responseCache The service for caching CHECK responses.
     * @param webClient     The configured Vert.x WebClient for making HTTP requests.
     * @param objectMapper  The Jackson ObjectMapper for JSON processing.
     */
    public DirectiveExecutor(CheckResponseCacheService responseCache, WebClient webClient, ObjectMapper objectMapper) {
        this.responseCache = responseCache;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Main entry point to execute an upsert directive. This method orchestrates the entire
     * CHECK-then-ACT flow asynchronously.
     *
     * @param directive        The upsert directive containing configuration for check, create, and update actions.
     * @param arcConfig        The configuration for the target ARC, containing action paths.
     * @param transformationId The ID of the parent transformation for logging and caching context.
     * @param targetApiUrl     The base URL of the target API.
     * @return A {@link Uni<Void>} that completes when the entire operation is finished or fails.
     */
    public Uni<Void> execute(UpsertDirective directive, RequestConfigurationMessageDTO arcConfig, Long transformationId, String targetApiUrl) {
        try (var ignored = MDC.putCloseable("transformationId", transformationId.toString())) {
            Log.infof("Processing directive: %s for target: %s", directive.get__directiveType(), targetApiUrl);

            return executeCheckRequest(directive, arcConfig, targetApiUrl)
                    .flatMap(checkResponse -> processCheckResponse(checkResponse, directive, arcConfig, transformationId, targetApiUrl))
                    .onFailure().invoke(failure ->
                            Log.errorf(failure, "A technical error occurred during directive execution for '%s'. Title: %s",
                                    directive.get__directiveType(),
                                    (failure instanceof SyncNodeException) ? ((SyncNodeException) failure).getTitle() : "Unknown")
                    )
                    .replaceWithVoid();
        }
    }

    /**
     * Handles the logic after the CHECK request completes, branching to the CREATE or UPDATE flow.
     *
     * @param checkResponse    The HTTP response from the initial CHECK request.
     * @param directive        The original upsert directive.
     * @param arcConfig        The target ARC configuration.
     * @param transformationId The current transformation ID.
     * @param targetApiUrl     The base URL of the target API.
     * @return A {@link Uni} representing the subsequent CREATE or UPDATE action.
     */
    private Uni<HttpResponse<Buffer>> processCheckResponse(HttpResponse<Buffer> checkResponse, UpsertDirective directive,
                                                           RequestConfigurationMessageDTO arcConfig, Long transformationId, String targetApiUrl) {
        String checkResponseBody = checkResponse.bodyAsString();
        int statusCode = checkResponse.statusCode();
        int acceptingStatusCodeLowerBound = 200;
        int acceptingStatusCodeUpperBound = 299;
        if (statusCode >= acceptingStatusCodeLowerBound && statusCode <= acceptingStatusCodeUpperBound) {
            responseCache.addResponse(transformationId, arcConfig.id(), checkResponseBody);
            Log.infof("CHECK successful (2XX accepted)");
            return executeUpdateRequest(directive, arcConfig, targetApiUrl, checkResponseBody);
        } else if (statusCode == Response.Status.NOT_FOUND.getStatusCode()) {
            Log.infof("CHECK returned 404 Not Found. Proceeding with CREATE.");
            return executeCreateRequest(directive, arcConfig, targetApiUrl);
        } else {
            Log.errorf("CHECK request failed with unexpected status code: %d. Response: %s", statusCode, checkResponseBody);
            return Uni.createFrom().nullItem(); // Gracefully stop the chain for this directive
        }
    }

    /**
     * Prepares and executes the initial CHECK (GET) request by calling the cached method.
     *
     * @param directive        The upsert directive containing the CHECK configuration.
     * @param arcConfig        The target ARC configuration.
     * @param targetApiUrl     The base URL of the target API.
     * @return A {@link Uni} that will emit the HTTP response of the CHECK request.
     */
    private Uni<HttpResponse<Buffer>> executeCheckRequest(UpsertDirective directive, RequestConfigurationMessageDTO arcConfig,
                                                          String targetApiUrl) {
        try {
            String pathTemplate = findPathForAction(arcConfig, TargetApiRequestConfigurationActionRole.CHECK)
                    .orElseThrow(() -> new IllegalStateException("CHECK action path is missing for ARC " + arcConfig.alias()));

            ApiCallConfiguration checkConfig = directive.getCheckConfiguration();
            String resolvedPath = resolvePathParameters(pathTemplate, checkConfig.getPathParameters());
            MultivaluedMap<String, String> queryParams = extractQueryParams(checkConfig);
            String fullUrlForCache = buildFullUrlForCache(targetApiUrl, resolvedPath, queryParams);

            return cachedCheckRequest(targetApiUrl, resolvedPath, queryParams, fullUrlForCache, arcConfig);
        } catch (IllegalStateException e) {
            return Uni.createFrom().failure(new SyncNodeException("Configuration Error", e.getMessage(), e));
        }
    }

    /**
     * Executes a GET request that is backed by a cache. This method is public to allow
     * Quarkus's caching interceptor to work on it.
     *
     * @param targetApiUrl     The base URL of the target API.
     * @param resolvedPath     The path with placeholders already resolved.
     * @param queryParams      The query parameters for the request.
     * @param fullUrlForCache  The full URL string used as part of the cache key.
     * @param arcConfig        The Api Request Configuration object with metadata about the request.
     * @return A {@link Uni} that emits the HTTP response.
     */
    @CacheResult(cacheName = "check-cache", keyGenerator = UrlCacheKeyGenerator.class)
    public Uni<HttpResponse<Buffer>> cachedCheckRequest(String targetApiUrl, String resolvedPath, MultivaluedMap<String, String> queryParams,
                                                        String fullUrlForCache, RequestConfigurationMessageDTO arcConfig) {
        Log.infof("Executing CACHED CHECK: GET %s", fullUrlForCache);
        return buildRequest(HttpMethod.GET, targetApiUrl, resolvedPath, queryParams, arcConfig.headers(), null)
                .flatMap(HttpRequest::send);
    }

    /**
     * Prepares and executes a CREATE (POST) request.
     *
     * @param directive    The upsert directive containing the CREATE configuration.
     * @param arcConfig    The target ARC configuration.
     * @param targetApiUrl The base URL of the target API.
     * @return A {@link Uni} that emits the HTTP response of the CREATE request.
     */
    private Uni<HttpResponse<Buffer>> executeCreateRequest(UpsertDirective directive, RequestConfigurationMessageDTO arcConfig, String targetApiUrl) {
        return Uni.createFrom().deferred(() -> {
            try {
                String pathTemplate = findPathForAction(arcConfig, TargetApiRequestConfigurationActionRole.CREATE)
                        .orElseThrow(() -> new IllegalStateException("CREATE action path is missing for ARC " + arcConfig.alias()));

                ApiCallConfiguration createConfig = directive.getCreateConfiguration();
                String resolvedPath = resolvePathParameters(pathTemplate, createConfig.getPathParameters());
                Buffer payloadBuffer = jsonToBuffer(createConfig.getPayload());

                Log.infof("Executing CREATE: POST %s", targetApiUrl + resolvedPath);
                return buildRequest(HttpMethod.POST, targetApiUrl, resolvedPath, new MultivaluedHashMap<>(), arcConfig.headers(), payloadBuffer)
                        .flatMap(request -> request.sendBuffer(payloadBuffer))
                        .invoke(response -> logWriteResponse("CREATE", response, payloadBuffer));
            } catch (RuntimeException e) {
                return Uni.createFrom().failure(new SyncNodeException("Create Request Failed", e.getMessage(), e));
            }
        });
    }

    /**
     * Prepares and executes an UPDATE (PUT) request. Catches and wraps potential exceptions during preparation.
     *
     * @param directive         The upsert directive containing the UPDATE configuration.
     * @param arcConfig         The target ARC configuration.
     * @param targetApiUrl      The base URL of the target API.
     * @param checkResponseBody The body of the preceding successful CHECK response, used to resolve path parameters.
     * @return A {@link Uni} that emits the HTTP response of the UPDATE request.
     */
    private Uni<HttpResponse<Buffer>> executeUpdateRequest(UpsertDirective directive, RequestConfigurationMessageDTO arcConfig,
                                                           String targetApiUrl, String checkResponseBody) {
        return Uni.createFrom().deferred(() -> {
            try {
                String pathTemplate = findPathForAction(arcConfig, TargetApiRequestConfigurationActionRole.UPDATE)
                        .orElseThrow(() -> new IllegalStateException("UPDATE action path is missing for ARC " + arcConfig.alias()));

                ApiCallConfiguration updateConfig = directive.getUpdateConfiguration();
                Buffer payloadBuffer = jsonToBuffer(updateConfig.getPayload());
                Map<String, Object> resolvedParams = resolveCheckResponsePlaceholders(updateConfig.getPathParameters(), checkResponseBody);
                String resolvedPath = resolvePathParameters(pathTemplate, resolvedParams);

                Log.infof("Executing UPDATE: PUT %s", targetApiUrl + resolvedPath);
                return buildRequest(HttpMethod.PUT, targetApiUrl, resolvedPath, new MultivaluedHashMap<>(), arcConfig.headers(), payloadBuffer)
                        .flatMap(request -> request.sendBuffer(payloadBuffer))
                        .invoke(response -> logWriteResponse("UPDATE", response, payloadBuffer));
            } catch (JsonProcessingException e) {
                return Uni.createFrom().failure(new SyncNodeException("Update Path Resolution Failed", "Could not parse CHECK response to resolve UPDATE path parameters.", e));
            } catch (RuntimeException e) {
                return Uni.createFrom().failure(new SyncNodeException("Update Request Failed", e.getMessage(), e));
            }
        });
    }

    /**
     * Centralized, safe method to create and configure a WebClient request object. This reduces duplication
     * and handles potential URI syntax errors gracefully within the reactive stream.
     *
     * @param method      The HTTP method (GET, POST, etc.).
     * @param baseApiUrl  The base URL of the API, including the scheme.
     * @param path        The resource path for the request.
     * @param queryParams Any query parameters to add.
     * @param headers     The list of predefined headers Key-Value pairs for the target system.
     * @param payload     The request body, or null if there is none.
     * @return A {@link Uni} that emits the configured {@link HttpRequest} or a failure with a {@link SyncNodeException} if the baseApiUrl is invalid.
     */
    private Uni<HttpRequest<Buffer>> buildRequest(HttpMethod method, String baseApiUrl, String path,
                                                  MultivaluedMap<String, String> queryParams,
                                                  List<ApiRequestHeaderMessageDTO> headers, Buffer payload) {
        try {
            URI uri = new URI(baseApiUrl);
            boolean useSsl = "https".equalsIgnoreCase(uri.getScheme());
            int port = uri.getPort() != -1 ? uri.getPort() : (useSsl ? 443 : 80);

            HttpRequest<Buffer> request = webClient.request(method, port, uri.getHost(), path).ssl(useSsl);
            queryParams.forEach((key, values) -> values.forEach(value -> request.addQueryParam(key, value)));
            headers.forEach(headerPair -> request.putHeader(headerPair.headerName(), headerPair.headerValue()));
            if (payload != null) {
                request.putHeader("Content-Type", "application/json");
            }
            return Uni.createFrom().item(request);
        } catch (URISyntaxException e) {
            return Uni.createFrom().failure(new SyncNodeException("Invalid URL", "The target API URL has invalid syntax: " + baseApiUrl, e));
        }
    }

    /**
     * Logs the outcome of a write operation (CREATE or UPDATE).
     *
     * @param action   A string identifying the action ("CREATE" or "UPDATE").
     * @param response The HTTP response received from the target system.
     * @param payload  The payload that was sent, for logging on failure.
     */
    private void logWriteResponse(String action, HttpResponse<Buffer> response, Buffer payload) {
        if (response.statusCode() >= 400) {
            Log.errorf("%s request failed with status code: %d. Response: %s. Payload: %s",
                    action, response.statusCode(), response.bodyAsString(), payload.toString());
        } else {
            Log.infof("%s successful with status: %d", action, response.statusCode());
        }
    }

    /**
     * Safely converts a Jackson {@link JsonNode} into a Vert.x {@link Buffer}.
     *
     * @param node The JsonNode to serialize.
     * @return The resulting Buffer.
     * @throws RuntimeException if serialization fails, wrapping the original {@link JsonProcessingException}.
     */
    private Buffer jsonToBuffer(JsonNode node) {
        try {
            return Buffer.buffer(objectMapper.writeValueAsBytes(node));
        } catch (JsonProcessingException e) {
            // This runtime exception will be caught by the deferred Uni wrapper.
            throw new RuntimeException("Failed to serialize payload to JSON buffer", e);
        }
    }

    /**
     * Constructs a full URL string for logging and caching purposes.
     *
     * @param targetApiUrl The base URL of the API.
     * @param resolvedPath The path with placeholders resolved.
     * @param queryParams  The map of query parameters.
     * @return The complete URL as a string.
     */
    private String buildFullUrlForCache(String targetApiUrl, String resolvedPath, MultivaluedMap<String, String> queryParams) {
        UriBuilder builder = UriBuilder.fromUri(targetApiUrl).path(resolvedPath);
        queryParams.forEach((key, values) -> builder.queryParam(key, values.toArray()));
        return builder.build().toString();
    }

    /**
     * Replaces placeholders in a path template (e.g., "/users/{id}") with actual values.
     *
     * @param pathTemplate   The path containing placeholders in the format "{key}".
     * @param pathParameters A map of placeholder keys to their values.
     * @return The resolved path string.
     */
    private String resolvePathParameters(String pathTemplate, Map<String, ?> pathParameters) {
        if (pathParameters == null || pathParameters.isEmpty()) {
            return pathTemplate;
        }
        String resolvedPath = pathTemplate;
        for (Map.Entry<String, ?> entry : pathParameters.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            Object value = entry.getValue();
            if (value != null) {
                resolvedPath = resolvedPath.replace(placeholder, String.valueOf(value));
            }
        }
        return resolvedPath;
    }

    /**
     * Resolves path parameter values from a previous CHECK response body using JSON Pointer expressions.
     *
     * @param pathParameterExpressions A map of parameter names to their value expressions (e.g., "{{checkResponse.body.user.id}}").
     * @param checkResponseBody        The JSON string response from the CHECK request.
     * @return A map of resolved parameter names to their found values.
     * @throws JsonProcessingException if the checkResponseBody is not valid JSON.
     */
    private Map<String, Object> resolveCheckResponsePlaceholders(Map<String, String> pathParameterExpressions, String checkResponseBody) throws JsonProcessingException {
        if (pathParameterExpressions == null || pathParameterExpressions.isEmpty()) {
            return Collections.emptyMap();
        }

        JsonNode checkResponseJson = objectMapper.readTree(checkResponseBody);
        Map<String, Object> resolvedParams = new HashMap<>();

        for (Map.Entry<String, String> entry : pathParameterExpressions.entrySet()) {
            String paramName = entry.getKey();
            String valueExpression = entry.getValue();

            if (valueExpression != null && valueExpression.startsWith("{{") && valueExpression.endsWith("}}")) {
                String path = valueExpression.substring("{{checkResponse.body.".length(), valueExpression.length() - "}}".length());
                String jsonPointer = "/" + path.replace('.', '/').replaceAll("\\[(\\d+)\\]", "/$1");

                JsonNode valueNode = checkResponseJson.at(jsonPointer);
                if (!valueNode.isMissingNode()) {
                    resolvedParams.put(paramName, valueNode.isNumber() ? valueNode.numberValue() : valueNode.asText());
                } else {
                    Log.warnf("Placeholder '%s' could not be resolved. JSON Pointer '%s' not found in CHECK response.", valueExpression, jsonPointer);
                    resolvedParams.put(paramName, null);
                }
            } else {
                resolvedParams.put(paramName, valueExpression);
            }
        }
        return resolvedParams;
    }

    /**
     * Finds the specific path for a given action role (CHECK, CREATE, UPDATE) from the ARC configuration.
     *
     * @param arcConfig The ARC configuration containing a list of possible actions.
     * @param role      The desired action role.
     * @return An {@link Optional} containing the path string if found.
     */
    private Optional<String> findPathForAction(RequestConfigurationMessageDTO arcConfig, TargetApiRequestConfigurationActionRole role) {
        return arcConfig.actions().stream()
                .filter(action -> action.actionRole() == role)
                .map(ActionMessageDTO::path)
                .findFirst();
    }

    /**
     * Extracts query parameters from an API call configuration object.
     *
     * @param config The API call configuration.
     * @return A {@link MultivaluedMap} of the query parameters.
     */
    private MultivaluedMap<String, String> extractQueryParams(ApiCallConfiguration config) {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        if (config != null && config.getParameters() != null) {
            config.getParameters().forEach((paramType, paramMap) -> {
                if ("query".equalsIgnoreCase(paramType) && paramMap instanceof Map) {
                    ((Map<?, ?>) paramMap).forEach((key, value) -> {
                        if (key != null && value != null) {
                            map.add(key.toString(), value.toString());
                        }
                    });
                }
            });
        }
        return map;
    }
}