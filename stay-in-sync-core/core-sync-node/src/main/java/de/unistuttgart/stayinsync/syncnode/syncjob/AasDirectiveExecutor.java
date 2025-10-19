package de.unistuttgart.stayinsync.syncnode.syncjob;

import de.unistuttgart.stayinsync.exception.SyncNodeException;
import de.unistuttgart.stayinsync.syncnode.domain.AasUpdateValueDirective;
import de.unistuttgart.stayinsync.transport.dto.targetsystems.AasTargetArcMessageDTO;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.MDC;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Executes directives specifically for updating values in an Asset Administration Shell (AAS) server.
 * <p>
 * This service is responsible for handling {@link AasUpdateValueDirective} objects. Its primary function
 * is to construct a compliant AAS REST API path for updating a submodel element's value and
 * execute the corresponding HTTP PATCH request. It handles the necessary URL and Base64 encoding
 * as specified by the AAS standards.
 */
@ApplicationScoped
public class AasDirectiveExecutor {

    private static final String AAS_UPDATE_PATH_TEMPLATE = "/submodels/%s/submodel-elements/%s/$value";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private final WebClient webClient;

    /**
     * Constructs the AasDirectiveExecutor with its required dependencies.
     *
     * @param webClient The configured Vert.x WebClient for making HTTP requests.
     */
    public AasDirectiveExecutor(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Executes the "update value" operation for a given AAS directive.
     * <p>
     * This method orchestrates the entire process: it builds the request path, prepares the payload,
     * creates and sends the HTTP PATCH request, and logs the outcome. The entire operation is
     * asynchronous and returns a {@link Uni}.
     *
     * @param directive        The directive containing the new value and the path to the element.
     * @param arcConfig        The configuration for the target AAS, including its base URL and submodel ID.
     * @param transformationId The ID of the parent transformation for logging context.
     * @return A {@link Uni<Void>} that completes when the operation is finished or fails.
     */
    public Uni<Void> executeUpdateValue(AasUpdateValueDirective directive, AasTargetArcMessageDTO arcConfig, Long transformationId) {
        try (var ignored = MDC.putCloseable("transformationId", String.valueOf(transformationId))) {
            return Uni.createFrom().deferred(() -> {
                        try {
                            String requestUriPath = buildAasUpdatePath(arcConfig, directive);
                            String jsonStringPayload = Json.encode(directive.getValue());
                            Buffer payloadBuffer = Buffer.buffer(jsonStringPayload);

                            Log.infof("Executing AAS Update: PATCH %s%s", arcConfig.baseUrl(), requestUriPath);

                            return executePatchRequest(arcConfig.baseUrl(), requestUriPath, payloadBuffer)
                                    .invoke(response -> logAasUpdateResponse(response, requestUriPath));

                        } catch (SyncNodeException e) {
                            return Uni.createFrom().failure(e);
                        }
                    })
                    .onFailure().invoke(failure ->
                            Log.errorf(failure, "AAS UPDATE request failed for element path '%s'. Title: %s",
                                    directive.getElementIdShortPath(),
                                    (failure instanceof SyncNodeException) ? ((SyncNodeException) failure).getTitle() : "Unknown")
                    )
                    .replaceWithVoid();
        }
    }

    /**
     * Constructs the specific URI path for the AAS "update value" API call.
     * <p>
     * This method performs two critical encoding steps required by the AAS specification:
     * <ul>
     *     <li>The Submodel ID is Base64 URL-encoded.</li>
     *     <li>The Submodel Element's ID-short path is URL-encoded to handle special characters safely.</li>
     * </ul>
     *
     * @param arcConfig The AAS target configuration.
     * @param directive The directive containing the element path.
     * @return The fully constructed and encoded request path.
     * @throws SyncNodeException if the element path cannot be URL-encoded.
     */
    private String buildAasUpdatePath(AasTargetArcMessageDTO arcConfig, AasUpdateValueDirective directive) throws SyncNodeException {
        String base64SubmodelId = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(arcConfig.submodelId().getBytes(StandardCharsets.UTF_8));

        // URL-encode the element path to safely handle characters like '/' or '?'
        String encodedElementPath = URLEncoder.encode(directive.getElementIdShortPath(), StandardCharsets.UTF_8);

        return String.format(AAS_UPDATE_PATH_TEMPLATE, base64SubmodelId, encodedElementPath);
    }

    /**
     * Creates and executes the HTTP PATCH request using the WebClient.
     *
     * @param baseUrl        The base URL of the AAS server.
     * @param requestUriPath The fully constructed and encoded request path.
     * @param payloadBuffer  The JSON payload as a Vert.x Buffer.
     * @return A {@link Uni} that will emit the HTTP response.
     */
    private Uni<HttpResponse<Buffer>> executePatchRequest(String baseUrl, String requestUriPath, Buffer payloadBuffer) {
        try {
            URI serverUri = new URI(baseUrl);
            int port = serverUri.getPort() != -1 ? serverUri.getPort() : ("https".equalsIgnoreCase(serverUri.getScheme()) ? 443 : 80);
            boolean useSsl = "https".equalsIgnoreCase(serverUri.getScheme());

            HttpRequest<Buffer> request = webClient.patch(port, serverUri.getHost(), requestUriPath)
                    .ssl(useSsl)
                    .putHeader(CONTENT_TYPE_HEADER, CONTENT_TYPE_JSON);

            return request.sendBuffer(payloadBuffer);
        } catch (URISyntaxException e) {
            return Uni.createFrom().failure(new SyncNodeException("Invalid URL", "The AAS base URL has invalid syntax: " + baseUrl, e));
        }
    }

    /**
     * Logs the outcome of the AAS update operation based on the HTTP response.
     *
     * @param response       The HTTP response from the AAS server.
     * @param requestUriPath The path that was targeted, for logging context.
     */
    private void logAasUpdateResponse(HttpResponse<Buffer> response, String requestUriPath) {
        if (response.statusCode() >= 400) {
            Log.errorf("AAS UPDATE request failed for path '%s' with status code: %d. Response: %s",
                    requestUriPath, response.statusCode(), response.bodyAsString());
        } else {
            Log.infof("AAS UPDATE success for path '%s' with status: %d",
                    requestUriPath, response.statusCode());
        }
    }
}