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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the writing of data to various target systems based on directives from a transformation script.
 * <p>
 * This service acts as the central dispatcher for all write operations. Its primary responsibilities are:
 * <ul>
 *     <li>Parsing the output of a {@link TransformationResult} to extract a list of directives.</li>
 *     <li>Flattening and preparing a worklist of "tasks" from these directives.</li>
 *     <li>Executing these tasks concurrently with a configurable rate limit to avoid overwhelming target systems.</li>
 *     <li>Dispatching each task to the appropriate executor (e.g., for standard REST APIs or AAS APIs) based on its type.</li>
 *     <li>Handling errors gracefully, ensuring that the failure of a single directive does not halt the processing of others.</li>
 *     <li>Recording metrics about the number of directives processed.</li>
 * </ul>
 */
@ApplicationScoped
public class TargetSystemWriterService {

    private final ObjectMapper objectMapper;
    private final int rateLimit;
    private final DirectiveExecutor restDirectiveExecutor;
    private final AasDirectiveExecutor aasDirectiveExecutor;
    private final Counter processedMessagesCounter;

    /**
     * A private record to encapsulate all necessary information for processing a single directive.
     */
    private record DirectiveTask(
            JsonNode directiveNode,
            String arcAlias,
            TransformationMessageDTO transformationContext
    ) {
    }

    /**
     * Constructs the TargetSystemWriterService with its required dependencies.
     *
     * @param objectMapper          The Jackson ObjectMapper for JSON processing.
     * @param rateLimit             The maximum number of directives to process concurrently.
     * @param restDirectiveExecutor The executor for standard REST "upsert" directives.
     * @param aasDirectiveExecutor  The executor for AAS-specific directives.
     * @param meterRegistry         The registry for creating and managing metrics.
     */
    public TargetSystemWriterService(ObjectMapper objectMapper,
                                     @ConfigProperty(name = "stayinsync.writer.rate-limit", defaultValue = "10") int rateLimit,
                                     DirectiveExecutor restDirectiveExecutor,
                                     AasDirectiveExecutor aasDirectiveExecutor,
                                     MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.rateLimit = rateLimit;
        this.restDirectiveExecutor = restDirectiveExecutor;
        this.aasDirectiveExecutor = aasDirectiveExecutor;
        this.processedMessagesCounter = Counter.builder("transformation_scripts_messages_total")
                .description("Total number of messages processed across all transformation scripts")
                .register(meterRegistry);
    }

    /**
     * Processes all directives found in a {@link TransformationResult}.
     * This is the main entry point for the service.
     *
     * @param result                The result from a script execution, containing the directive data.
     * @param transformationContext The context of the transformation, containing ARC configurations.
     * @return A {@link Uni<Void>} that completes when all directives have been processed.
     */
    public Uni<Void> processDirectives(TransformationResult result, TransformationMessageDTO transformationContext) {
        try (var ignored = MDC.putCloseable("transformationId", transformationContext.id().toString())) {
            Map<String, List<JsonNode>> directiveMap = parseDirectives(result);
            if (directiveMap.isEmpty()) {
                Log.infof("No directives to process for job %s.", result.getJobId());
                return Uni.createFrom().voidItem();
            }

            List<DirectiveTask> allTasks = flattenDirectivesToTasks(directiveMap, transformationContext);
            if (allTasks.isEmpty()) {
                Log.infof("No directives to process for job %s.", result.getJobId());
                return Uni.createFrom().voidItem();
            }

            return executeTasksConcurrently(allTasks, result.getJobId());
        } catch (Exception e) {
            Log.errorf(e, "An unexpected error occurred during the initial setup of directive processing for job %s.", result.getJobId());
            return Uni.createFrom().failure(e);
        }
    }

    /**
     * Parses the output data from a {@link TransformationResult} into a structured map of directives.
     *
     * @param result The transformation result.
     * @return A map where keys are ARC aliases and values are lists of directive JSON nodes. Returns an empty map if parsing fails or data is null.
     */
    private Map<String, List<JsonNode>> parseDirectives(TransformationResult result) {
        if (result.getOutputData() == null) {
            return Map.of();
        }
        try {
            return objectMapper.convertValue(result.getOutputData(), new TypeReference<>() {
            });
        } catch (IllegalArgumentException e) {
            Log.errorf(e, "Failed to parse directives from script output for job %s. Output was not a valid directive map.", result.getJobId());
            return Map.of();
        }
    }

    /**
     * Flattens the directive map into a single list of {@link DirectiveTask} objects for processing.
     * This method also increments the processed messages counter for each directive found.
     *
     * @param directiveMap          The map of directives parsed from the script output.
     * @param transformationContext The context of the transformation.
     * @return A flat list of all tasks to be executed.
     */
    private List<DirectiveTask> flattenDirectivesToTasks(Map<String, List<JsonNode>> directiveMap, TransformationMessageDTO transformationContext) {
        List<DirectiveTask> allTasks = new ArrayList<>();
        directiveMap.forEach((arcAlias, directives) -> {
            for (JsonNode directiveNode : directives) {
                allTasks.add(new DirectiveTask(directiveNode, arcAlias, transformationContext));
                processedMessagesCounter.increment();
            }
        });
        return allTasks;
    }

    /**
     * Executes a list of {@link DirectiveTask}s concurrently using a reactive stream with a defined rate limit.
     *
     * @param tasks The list of tasks to execute.
     * @param jobId The parent job ID, for logging purposes.
     * @return A {@link Uni<Void>} that completes when all tasks in the stream have been processed.
     */
    private Uni<Void> executeTasksConcurrently(List<DirectiveTask> tasks, String jobId) {
        Log.infof("Starting processing of %d directives for job %s with a concurrency limit of %d.", tasks.size(), jobId, rateLimit);

        return Multi.createFrom().iterable(tasks)
                .onItem().transformToUni(this::safelyDispatchAndExecute)
                .merge(rateLimit) // This operator controls the concurrency level.
                .collect().asList()
                .onItem().invoke(results -> {
                    // Count the amount of successful (true) results from the triggered tasks
                    long successCount = results.stream().filter(Boolean::booleanValue).count();
                    Log.infof("Finished processing all directives for job %s. Total tasks: %d, Succeeded: %d, Failed (recovered): %d",
                            jobId, results.size(), successCount, results.size() - successCount);
                })
                .replaceWithVoid();
    }

    /**
     * A safe wrapper around the dispatch logic that handles synchronous configuration errors gracefully
     * and returns a Uni<Boolean> representing the outcome.
     *
     * @param task The task to execute.
     * @return A {@link Uni<Boolean>} emitting {@code true} on success, or {@code false} on a recovered failure.
     */
    private Uni<Boolean> safelyDispatchAndExecute(DirectiveTask task) {
        try {
            return dispatchAndExecute(task)
                    // On successful completion of Uni<Void>, transform it to a Uni emitting true.
                    .map(v -> true)
                    .onFailure().recoverWithItem(failure -> {
                        // This handles asynchronous errors during the Uni's execution (e.g., network issues).
                        Log.errorf(failure, "A recoverable execution error occurred for a directive for ARC '%s'. Processing continues.", task.arcAlias());
                        return false; // Return false to signify a recovered failure.
                    });
        } catch (SyncNodeException e) {
            // This handles synchronous errors during setup (e.g., ARC config not found).
            Log.errorf("A recoverable configuration error occurred: %s. This directive was skipped, but processing continues.", e.getMessage());
            return Uni.createFrom().item(false); // Return a Uni emitting false for a synchronous failure.
        }
    }

    /**
     * Dispatches a single task to the appropriate executor based on its directive type.
     *
     * @param task The directive task to be executed.
     * @return A {@link Uni<Void>} representing the asynchronous execution of the directive.
     * @throws SyncNodeException if the directive type is unknown, the ARC configuration is missing, or JSON conversion fails.
     */
    private Uni<Void> dispatchAndExecute(DirectiveTask task) throws SyncNodeException {
        JsonNode directiveNode = task.directiveNode();
        String directiveType = directiveNode.has("__directiveType") ? directiveNode.get("__directiveType").asText() : "unknown";

        if (directiveType.equals("AasUpdateValueDirective")) {
            AasTargetArcMessageDTO aasArcConfig = findAasArcConfig(task.transformationContext(), task.arcAlias());
            AasUpdateValueDirective aasDirective = convertValue(directiveNode, AasUpdateValueDirective.class);
            return aasDirectiveExecutor.executeUpdateValue(aasDirective, aasArcConfig, task.transformationContext().id());
        }
        if (directiveType.endsWith("_UpsertDirective")) {
            RequestConfigurationMessageDTO genericArcConfig = findRestArcConfig(task.transformationContext(), task.arcAlias());
            UpsertDirective genericDirective = convertValue(directiveNode, UpsertDirective.class);
            return restDirectiveExecutor.execute(genericDirective, genericArcConfig, task.transformationContext().id(), genericArcConfig.baseUrl());
        }
        Log.warnf("Unknown directive type '%s' for arc '%s'. Skipping task.", directiveType, task.arcAlias());
        return Uni.createFrom().voidItem();
    }

    /**
     * Finds the REST ARC configuration for a given alias within the transformation context.
     *
     * @param context The transformation context.
     * @param alias   The ARC alias to find.
     * @return The found {@link RequestConfigurationMessageDTO}.
     * @throws SyncNodeException if no configuration is found for the given alias.
     */
    private RequestConfigurationMessageDTO findRestArcConfig(TransformationMessageDTO context, String alias) throws SyncNodeException {
        return context.targetRequestConfigurationMessageDTOS().stream()
                .filter(c -> c.alias().equals(alias))
                .findFirst()
                .orElseThrow(() -> new SyncNodeException("Target ARC Configuration Error",
                        "Requested REST ARC alias is not present in the provided transformation context: " + alias));
    }

    /**
     * Finds the AAS ARC configuration for a given alias within the transformation context.
     *
     * @param context The transformation context.
     * @param alias   The ARC alias to find.
     * @return The found {@link AasTargetArcMessageDTO}.
     * @throws SyncNodeException if no configuration is found for the given alias.
     */
    private AasTargetArcMessageDTO findAasArcConfig(TransformationMessageDTO context, String alias) throws SyncNodeException {
        return context.aasTargetRequestConfigurationMessageDTOS().stream()
                .filter(c -> c.alias().equals(alias))
                .findFirst()
                .orElseThrow(() -> new SyncNodeException("Target AAS ARC Config Error", "AAS config not found for alias: " + alias));
    }

    /**
     * A generic and safe helper to convert a JsonNode to a specific class type.
     *
     * @param node  The JsonNode to convert.
     * @param clazz The target class.
     * @return An instance of the target class.
     * @throws SyncNodeException if the conversion fails.
     */
    private <T> T convertValue(JsonNode node, Class<T> clazz) throws SyncNodeException {
        try {
            return objectMapper.convertValue(node, clazz);
        } catch (IllegalArgumentException e) {
            throw new SyncNodeException("Directive Deserialization Error", "Failed to convert JSON to " + clazz.getSimpleName(), e);
        }
    }
}