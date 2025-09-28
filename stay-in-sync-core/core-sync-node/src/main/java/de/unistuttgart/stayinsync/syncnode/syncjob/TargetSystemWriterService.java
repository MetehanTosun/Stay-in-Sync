package de.unistuttgart.stayinsync.syncnode.syncjob;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.exception.SyncNodeException;
import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.syncnode.domain.AasUpdateValueDirective;
import de.unistuttgart.stayinsync.syncnode.domain.UpsertDirective;
import de.unistuttgart.stayinsync.transport.dto.TransformationMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.targetsystems.AasTargetArcMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.targetsystems.RequestConfigurationMessageDTO;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TargetSystemWriterService {

    @ConfigProperty(name = "stayinsync.writer.rate-limit", defaultValue = "10")
    int rateLimit;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    DirectiveExecutor restDirectiveExecutor;

    @Inject
    AasDirectiveExecutor aasDirectiveExecutor;

    private record DirectiveTask(
            JsonNode directiveNode,
            String arcAlias,
            TransformationMessageDTO transformationContext
    ){}

    public Uni<Void> processDirectives(TransformationResult result, TransformationMessageDTO transformationContext) {
        try {
            MDC.put("transformationId", transformationContext.id().toString());

            Map<String, List<JsonNode>> directiveMap = objectMapper.convertValue(
                    result.getOutputData(),
                    new TypeReference<>() {}
            );

            if (directiveMap == null || directiveMap.isEmpty()) {
                Log.infof("Directive map is null or empty. No directives to process for job %s.", result.getJobId());
                return Uni.createFrom().voidItem();
            }

            List<DirectiveTask> allTasks = new ArrayList<>();
            directiveMap.forEach((arcAlias, directives) -> {
                for(JsonNode directiveNode : directives){
                    allTasks.add(new DirectiveTask(directiveNode, arcAlias, transformationContext));
                }
            });

            if (allTasks.isEmpty()) {
                Log.infof("No directives to process for job %s.", result.getJobId());
                return Uni.createFrom().voidItem();
            }

            Log.infof("Starting processing of %d directives for job %s with a concurrency limit of %d.",
                    allTasks.size(), result.getJobId(), rateLimit);

            return Multi.createFrom().iterable(allTasks)
                    .onItem().transformToUni(task -> {
                        try {
                            return dispatchAndExecute(task)
                                    .onFailure().recoverWithItem(failure -> {
                                        Log.errorf(failure, "A recoverable execution error occurred for a directive for ARC '%s'. Processing continues.", task.arcAlias());
                                        return null;
                                    });
                        } catch (SyncNodeException e) {
                            Log.errorf("A recoverable configuration error occurred: %s. This directive was skipped, but processing continues.", e.getMessage());
                            return Uni.createFrom().nullItem();
                        }
                    })
                    .merge(rateLimit)
                    .collect().asList()
                    .onItem().invoke(results -> Log.infof("Finished processing all directives for job %s. Processed items: %d",
                            result.getJobId(), results.size()))
                    .replaceWithVoid();

        } catch (Exception e) {
            Log.errorf(e, "An unexpected error occurred during the initial setup of directive processing for job %s.", result.getJobId());
            return Uni.createFrom().failure(e);
        } finally {
            MDC.remove("transformationId");
        }
    }

    private Uni<Void> dispatchAndExecute(DirectiveTask task) throws SyncNodeException {
        JsonNode directiveNode = task.directiveNode();
        String directiveType = directiveNode.has("__directiveType") ? directiveNode.get("__directiveType").asText() : "unknown";

        if (directiveType.endsWith("_UpsertDirective")) {
            RequestConfigurationMessageDTO arcConfig = findRestArcConfig(task.transformationContext(), task.arcAlias());
            UpsertDirective directive = objectMapper.convertValue(directiveNode, UpsertDirective.class);
            return restDirectiveExecutor.execute(directive, arcConfig, task.transformationContext().id(), arcConfig.baseUrl());
        }

        if ("AasUpdateValueDirective".equals(directiveType)) {
            AasTargetArcMessageDTO arcConfig = findAasArcConfig(task.transformationContext(), task.arcAlias());
            AasUpdateValueDirective directive = objectMapper.convertValue(directiveNode, AasUpdateValueDirective.class);
            return aasDirectiveExecutor.executeUpdateValue(directive, arcConfig, task.transformationContext().id());
        }

        Log.warnf("Unknown directive type '%s' for arc '%s'. Skipping task.", directiveType, task.arcAlias());
        return Uni.createFrom().voidItem();
    }

    private RequestConfigurationMessageDTO findRestArcConfig(TransformationMessageDTO transformationContext, String arcAlias) throws SyncNodeException {
        return transformationContext.targetRequestConfigurationMessageDTOS().stream()
                .filter(c -> c.alias().equals(arcAlias))
                .findFirst()
                .orElseThrow(() -> new SyncNodeException("Target ARC Configuration Error",
                        "Requested ARC alias is not present in the provided transformation context: " + arcAlias));
    }

    private AasTargetArcMessageDTO findAasArcConfig(TransformationMessageDTO context, String alias) throws SyncNodeException {
        return context.aasTargetRequestConfigurationMessageDTOS().stream()
                .filter(c -> c.alias().equals(alias))
                .findFirst()
                .orElseThrow(() -> new SyncNodeException("Target AAS ARC Config Error", "Config not found for alias: " + alias));
    }
}
