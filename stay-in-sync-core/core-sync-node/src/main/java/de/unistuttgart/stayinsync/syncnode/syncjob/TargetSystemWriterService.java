package de.unistuttgart.stayinsync.syncnode.syncjob;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.exception.SyncNodeException;
import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.syncnode.domain.UpsertDirective;
import de.unistuttgart.stayinsync.transport.dto.TransformationMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.targetsystems.RequestConfigurationMessageDTO;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TargetSystemWriterService {

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "stayinsync.writer.rate-limit", defaultValue = "10")
    int rateLimit;

    @Inject
    DirectiveExecutor directiveExecutor;

    private record DirectiveWithContext(
            UpsertDirective directive,
            RequestConfigurationMessageDTO arcConfig,
            String targetApiUrl) {
    }

    public Uni<Void> processDirectives(TransformationResult result, TransformationMessageDTO transformationContext) {
        Map<String, List<UpsertDirective>> directiveMap = objectMapper.convertValue(
                result.getOutputData(),
                new TypeReference<>() {}
        );

        if (directiveMap == null || directiveMap.isEmpty()) {
            Log.infof("Directive map is null or empty. No directives to process for job %s.", result.getJobId());
            return Uni.createFrom().voidItem();
        }
        List<DirectiveWithContext> allTasks = new ArrayList<>();
        directiveMap.forEach((arcAlias, directives) -> {
            try {
                RequestConfigurationMessageDTO arcConfig = findArcConfig(transformationContext, arcAlias);
                final String targetApiUrl = arcConfig.baseUrl();

                if (targetApiUrl == null || targetApiUrl.isBlank()) {
                    Log.errorf("TID: %d - Skipping ARC '%s' because its base URL is missing in the configuration.",
                            transformationContext.id(), arcAlias);
                    return;
                }

                for (UpsertDirective directive : directives) {
                    allTasks.add(new DirectiveWithContext(directive, arcConfig, targetApiUrl));
                }
            } catch (SyncNodeException e) {
                Log.errorf("Malformed ARC Configuration for alias '%s': %s. Skipping all directives for this ARC.", arcAlias, e.getMessage());
                // TODO: Position to Cancel Transformation indefinitely
            }
        });

        if (allTasks.isEmpty()) {
            Log.infof("No directives to process for job %s.", result.getJobId());
            return Uni.createFrom().voidItem();
        }

        Log.infof("Starting processing of %d directives for job %s with a concurrency limit of %d.",
                allTasks.size(), result.getJobId(), rateLimit);

        return Multi.createFrom().iterable(allTasks)
                .onItem().transformToUni(task ->
                        directiveExecutor.execute(
                                        task.directive(),
                                        task.arcConfig(),
                                        transformationContext.id(),
                                        task.targetApiUrl())
                                .onFailure().recoverWithItem(failure -> {
                                    Log.errorf(failure, "A single directive execution failed but processing will continue for job %s.", result.getJobId());
                                    return null;
                                })
                )
                .merge(rateLimit)
                .collect().asList()
                .onItem().invoke(results -> Log.infof("Finished processing all directives for job %s. Processed items: %d",
                        result.getJobId(), results.size()))
                .replaceWithVoid();
    }

    private RequestConfigurationMessageDTO findArcConfig(TransformationMessageDTO transformationContext, String arcAlias) throws SyncNodeException {
        return transformationContext.targetRequestConfigurationMessageDTOS().stream()
                .filter(c -> c.alias().equals(arcAlias))
                .findFirst()
                .orElseThrow(() -> new SyncNodeException("Target ARC Configuration Error",
                        "Requested ARC alias is not present in the provided transformation context: " + arcAlias));
    }
}
