package de.unistuttgart.stayinsync.syncnode.syncjob;

import de.unistuttgart.stayinsync.syncnode.domain.AasUpdateValueDirective;
import de.unistuttgart.stayinsync.transport.dto.targetsystems.AasTargetArcMessageDTO;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.MDC;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class AasDirectiveExecutor {

    @Inject
    WebClientProvider webClientProvider;

    public Uni<Void> executeUpdateValue(AasUpdateValueDirective directive, AasTargetArcMessageDTO arcConfig, Long transformationId){
        MDC.put("transformationId", String.valueOf(transformationId));
        WebClient client = webClientProvider.getClient();

        try {
            String encodedPath = URLEncoder.encode(directive.getElementIdShortPath(), StandardCharsets.UTF_8);
            String submodelId = URLEncoder.encode(arcConfig.submodelId(), StandardCharsets.UTF_8);

            String fullPath = String.format("/submodels/%s/submodel-elements/%s/value", submodelId, encodedPath);

            Log.infof("TID: %d - Executing AAS Update: PATCH %s%s", transformationId, arcConfig.baseUrl(), fullPath);

            return client.patchAbs(arcConfig.baseUrl() + fullPath)
                    .putHeader("Content-Type", "application/json")
                    .sendBuffer(Buffer.buffer(Json.encode(directive.getValue())))
                    .onItem().invoke(response -> {
                        MDC.put("transformationId", String.valueOf(transformationId));
                        if(response.statusCode() >= 400) {
                            Log.errorf("TID: %d - AAS UPDATE request failed for path '%s' with status code: %d. Response: %s",
                                    transformationId, fullPath, response.statusCode(), response.bodyAsString());
                        } else {
                            Log.infof("TID: %d - AAS UPDATE success for path '%s' with status: %d",
                                    transformationId, fullPath, response.statusCode());
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
