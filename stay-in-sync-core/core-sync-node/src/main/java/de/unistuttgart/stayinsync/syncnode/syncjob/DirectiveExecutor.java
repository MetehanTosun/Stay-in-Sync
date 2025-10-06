package de.unistuttgart.stayinsync.syncnode.syncjob;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.syncnode.domain.ApiCallConfiguration;
import de.unistuttgart.stayinsync.syncnode.domain.UpsertDirective;
import de.unistuttgart.stayinsync.syncnode.syncjob.assets.CheckResponseCacheService;
import de.unistuttgart.stayinsync.transport.domain.TargetApiRequestConfigurationActionRole;
import de.unistuttgart.stayinsync.transport.dto.targetsystems.ActionMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.targetsystems.RequestConfigurationMessageDTO;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.slf4j.MDC;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class DirectiveExecutor {

    @Inject
    CheckResponseCacheService responseCache;

    @Inject
    WebClientProvider webClientProvider;

    @Inject
    ObjectMapper objectMapper;

    public Uni<Void> execute(UpsertDirective directive, RequestConfigurationMessageDTO arcConfig, Long transformationId, String targetApiUrl) {
        MDC.put("transformationId", transformationId.toString());
        WebClient client = webClientProvider.getClient();
        Log.infof("TID: %d - Processing directive: %s for target: %s", transformationId, directive.get__directiveType(), targetApiUrl);

        return executeCheck(client, directive, arcConfig, transformationId, targetApiUrl)
                .onItem().transformToUni(checkResponse -> {
                    String checkResponseBody = checkResponse.bodyAsString();
                    int statusCode = checkResponse.statusCode();

                    if (statusCode == Response.Status.OK.getStatusCode()) {
                        responseCache.addResponse(transformationId, arcConfig.id(), checkResponseBody);
                        Log.infof("TID: %d - CHECK successful (200 OK), entity exists. Proceeding with UPDATE.", transformationId);
                        return handleUpdate(client, directive, arcConfig, transformationId, targetApiUrl, checkResponseBody);
                    } else if (statusCode == Response.Status.NOT_FOUND.getStatusCode()) {
                        Log.infof("TID: %d - CHECK returned 404 Not Found. Proceeding with CREATE.", transformationId);
                        return handleCreate(client, directive, arcConfig, transformationId, targetApiUrl);
                    } else {
                        Log.errorf("TID: %d - CHECK request failed with unexpected status code: %d. Response: %s",
                                transformationId, statusCode, checkResponseBody);
                        return Uni.createFrom().nullItem();
                    }
                })
                .onFailure().invoke(failure -> {
                    MDC.put("transformationId", transformationId.toString());
                    Log.errorf(failure, "TID: %d - A technical error occurred during directive execution for '%s'",
                            transformationId, directive.get__directiveType());
                })
                .replaceWithVoid();
    }

    @CacheResult(cacheName = "check-cache", keyGenerator = UrlCacheKeyGenerator.class)
    public Uni<HttpResponse<Buffer>> cachedCheckRequest(WebClient client, String targetApiUrl, String resolvedPath, MultivaluedMap<String, String> queryParams, String fullUrlForCache, Long transformationId) {
        MDC.put("transformationId", transformationId.toString());
        try {
            URI uri = new URI(targetApiUrl);
            boolean useSsl = "https".equalsIgnoreCase(uri.getScheme());
            int port = uri.getPort();
            if (port == -1) {
                port = useSsl ? 443 : 80;
            }

            HttpRequest<Buffer> request = client.get(port, uri.getHost(), resolvedPath)
                    .ssl(useSsl);

            queryParams.forEach((key, values) -> values.forEach(value -> request.addQueryParam(key, value)));

            Log.infof("TID: %d - Executing CACHED CHECK: GET %s", transformationId, fullUrlForCache);
            return request.send();
        } catch (URISyntaxException e) {
            Log.errorf(e, "TID: %d - Invalid Target API URL syntax: %s", transformationId, targetApiUrl);
            return Uni.createFrom().failure(e);
        }
    }

    private Uni<HttpResponse<Buffer>> executeCheck(WebClient client, UpsertDirective directive, RequestConfigurationMessageDTO arcConfig, Long transformationId, String targetApiUrl) {
        String pathTemplate = findPathForAction(arcConfig, TargetApiRequestConfigurationActionRole.CHECK)
                .orElseThrow(() -> new IllegalStateException("CHECK action path is missing for ARC " + arcConfig.alias()));

        ApiCallConfiguration checkConfig = directive.getCheckConfiguration();
        String resolvedPath = resolvePathParameters(pathTemplate, checkConfig.getPathParameters());
        MultivaluedMap<String, String> queryParams = extractQueryParams(checkConfig);

        String fullUrlForCache = buildFullUrlForCache(targetApiUrl, resolvedPath, queryParams);

        return cachedCheckRequest(client, targetApiUrl, resolvedPath, queryParams, fullUrlForCache, transformationId);
    }

    private Uni<HttpResponse<Buffer>> handleCreate(WebClient client, UpsertDirective directive, RequestConfigurationMessageDTO arcConfig, Long transformationId, String targetApiUrl) {
        MDC.put("transformationId", transformationId.toString());
        try {
            String pathTemplate = findPathForAction(arcConfig, TargetApiRequestConfigurationActionRole.CREATE)
                    .orElseThrow(() -> new IllegalStateException("CREATE action path is missing for ARC " + arcConfig.alias()));

            ApiCallConfiguration createConfig = directive.getCreateConfiguration();
            String resolvedPath = resolvePathParameters(pathTemplate, createConfig.getPathParameters());
            JsonNode payload = createConfig.getPayload();
            Buffer payloadBuffer = Buffer.buffer(objectMapper.writeValueAsBytes(payload));

            URI uri = new URI(targetApiUrl);
            boolean useSsl = "https".equalsIgnoreCase(uri.getScheme());
            int port = uri.getPort();
            if (port == -1) {
                port = useSsl ? 443 : 80;
            }

            HttpRequest<Buffer> request = client.post(port, uri.getHost(), resolvedPath)
                    .ssl(useSsl)
                    .putHeader("Content-Type", "application/json");

            Log.infof("TID: %d - Executing CREATE: POST %s", transformationId, targetApiUrl + resolvedPath);
            return request.sendBuffer(payloadBuffer)
                    .onItem().invoke(createResponse -> {
                        MDC.put("transformationId", transformationId.toString());
                        if (createResponse.statusCode() >= 400) {
                            Log.errorf("TID: %d - CREATE request failed with status code: %d. Response: %s. Payload: %s",
                                    transformationId, createResponse.statusCode(), createResponse.bodyAsString(), payload.toString());
                        } else {
                            Log.infof("TID: %d - CREATE successful with status: %d", transformationId, createResponse.statusCode());
                        }
                    });
        } catch (Exception e) {
            Log.errorf(e, "TID: %d - Failed to prepare CREATE request.", transformationId);
            return Uni.createFrom().failure(e);
        }
    }

    private Uni<HttpResponse<Buffer>> handleUpdate(WebClient client, UpsertDirective directive, RequestConfigurationMessageDTO arcConfig, Long transformationId, String targetApiUrl, String checkResponseBody) {
        MDC.put("transformationId", transformationId.toString());
        try {
            String pathTemplate = findPathForAction(arcConfig, TargetApiRequestConfigurationActionRole.UPDATE)
                    .orElseThrow(() -> new IllegalStateException("UPDATE action path is missing for ARC " + arcConfig.alias()));

            ApiCallConfiguration updateConfig = directive.getUpdateConfiguration();
            JsonNode payload = updateConfig.getPayload();
            Buffer payloadBuffer = Buffer.buffer(objectMapper.writeValueAsBytes(payload));

            Map<String, Object> resolvedParams = resolveCheckResponsePlaceholders(updateConfig.getPathParameters(), checkResponseBody);
            String resolvedPath = resolvePathParameters(pathTemplate, resolvedParams);

            URI uri = new URI(targetApiUrl);
            boolean useSsl = "https".equalsIgnoreCase(uri.getScheme());
            int port = uri.getPort();
            if (port == -1) {
                port = useSsl ? 443 : 80;
            }

            HttpRequest<Buffer> request = client.put(port, uri.getHost(), resolvedPath)
                    .ssl(useSsl)
                    .putHeader("Content-Type", "application/json");

            Log.infof("TID: %d - Executing UPDATE: PUT %s", transformationId, targetApiUrl + resolvedPath);
            return request.sendBuffer(payloadBuffer)
                    .onItem().invoke(updateResponse -> {
                        MDC.put("transformationId", transformationId.toString());
                        if (updateResponse.statusCode() >= 400) {
                            Log.errorf("TID: %d - UPDATE request failed with status code: %d. Response: %s. Payload: %s",
                                    transformationId, updateResponse.statusCode(), updateResponse.bodyAsString(), payload.toString());
                        } else {
                            Log.infof("TID: %d - UPDATE successful with status: %d", transformationId, updateResponse.statusCode());
                        }
                    });
        } catch (Exception e) {
            Log.errorf(e, "TID: %d - Failed to prepare or execute UPDATE request.", transformationId);
            return Uni.createFrom().failure(e);
        }
    }

    private String buildFullUrlForCache(String targetApiUrl, String resolvedPath, MultivaluedMap<String, String> queryParams) {
        UriBuilder builder = UriBuilder.fromUri(targetApiUrl).path(resolvedPath);
        queryParams.forEach((key, values) -> builder.queryParam(key, values.toArray()));
        return builder.build().toString();
    }

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
                String jsonPointer = valueExpression.substring(2, valueExpression.length() - 2)
                        .replace("checkResponse.body.", "/");
                JsonNode valueNode = checkResponseJson.at(jsonPointer);
                if (!valueNode.isMissingNode()) {
                    resolvedParams.put(paramName, valueNode.asText());
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

    private Optional<String> findPathForAction(RequestConfigurationMessageDTO arcConfig, TargetApiRequestConfigurationActionRole role) {
        return arcConfig.actions().stream()
                .filter(action -> action.actionRole() == role)
                .map(ActionMessageDTO::path)
                .findFirst();
    }

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
