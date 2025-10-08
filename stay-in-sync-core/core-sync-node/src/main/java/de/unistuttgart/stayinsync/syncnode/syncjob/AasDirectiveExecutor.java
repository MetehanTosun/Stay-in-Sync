package de.unistuttgart.stayinsync.syncnode.syncjob;

import de.unistuttgart.stayinsync.syncnode.domain.AasUpdateValueDirective;
import de.unistuttgart.stayinsync.transport.dto.targetsystems.AasTargetArcMessageDTO;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.MDC;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@ApplicationScoped
public class AasDirectiveExecutor {

    @Inject
    WebClientProvider webClientProvider;

    public Uni<Void> executeUpdateValue(AasUpdateValueDirective directive, AasTargetArcMessageDTO arcConfig, Long transformationId){
        MDC.put("transformationId", String.valueOf(transformationId));
        WebClient client = webClientProvider.getClient();

        try {
            String base64SubmodelId = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(arcConfig.submodelId().getBytes(StandardCharsets.UTF_8));
            String elementPath = directive.getElementIdShortPath();

            String requestUriPath = String.format("/submodels/%s/submodel-elements/%s/$value", base64SubmodelId, elementPath);

            Log.infof("TID: %d - Executing AAS Update: PATCH %s%s", transformationId, arcConfig.baseUrl(), requestUriPath);

            URI serverUri = new URI(arcConfig.baseUrl());
            int port = serverUri.getPort() != -1 ? serverUri.getPort() : ("https".equalsIgnoreCase(serverUri.getScheme()) ? 443 : 80);
            boolean useSsl = "https".equalsIgnoreCase(serverUri.getScheme());

            HttpRequest<Buffer> request = client.patch(port, serverUri.getHost(), requestUriPath)
                    .ssl(useSsl)
                    .putHeader("Content-Type", "application/json");

            String jsonStringPayload = Json.encode(directive.getValue());

            return request.sendBuffer(Buffer.buffer(jsonStringPayload))
                    .onItem().invoke(response -> {
                        MDC.put("transformationId", String.valueOf(transformationId));
                        if(response.statusCode() >= 400) {
                            Log.errorf("TID: %d - AAS UPDATE request failed for path '%s' with status code: %d. Response: %s",
                                    transformationId, requestUriPath, response.statusCode(), response.bodyAsString());
                        } else {
                            Log.infof("TID: %d - AAS UPDATE success for path '%s' with status: %d",
                                    transformationId, requestUriPath, response.statusCode());
                        }
                    })
                    .replaceWithVoid();
        } catch (Exception e) {
            Log.errorf(e, "TID: %d - Failed to prepare AAS UPDATE request for path '%s'",
                    transformationId, directive.getElementIdShortPath());
            return Uni.createFrom().failure(e);
        }
    }
}
