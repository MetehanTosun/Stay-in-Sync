package de.unistuttgart.stayinsync.syncnode.syncjob;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.stayinsync.scriptengine.ScriptEngineService;
import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.syncnode.LogicGraph.GraphHasher;
import de.unistuttgart.stayinsync.syncnode.LogicGraph.GraphInstanceCache;
import de.unistuttgart.stayinsync.syncnode.LogicGraph.StatefulLogicGraph;
import de.unistuttgart.stayinsync.syncnode.SnapshotManagement.SnapshotFactory;
import de.unistuttgart.stayinsync.syncnode.SnapshotManagement.SnapshotStore;
import de.unistuttgart.stayinsync.syncnode.domain.ExecutionPayload;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;

/**
 * Orchestrates the entire execution flow of a transformation job.
 * <p>
 * This service is the main engine that drives a transformation from start to finish. It follows a strict,
 * conditional execution pipeline:
 * <ol>
 *     <li><b>Graph Evaluation:</b> A logic graph is evaluated to produce a boolean outcome. This is the primary
 *         gatekeeper for the entire process, used for data comparison and change detection.</li>
 *     <li><b>Metric Initialization:</b> If the graph evaluates to {@code true}, performance metrics are started.</li>
 *     <li><b>User Script Execution:</b> The main user-provided transformation script is executed. This script is
 *         responsible for generating a list of "directives" for what actions to perform.</li>
 *     <li><b>Directive Execution:</b> The service iterates through the generated directives and executes the
 *         corresponding HTTP web requests to target systems.</li>
 * </ol>
 * The process is modeled reactively to ensure non-blocking execution, especially for the web requests.
 */
@ApplicationScoped
public class TransformationExecutionService {

    private final GraphInstanceCache graphCache;
    private final ScriptEngineService scriptEngineService;
    private final TargetSystemWriterService targetSystemWriterService;
    private final ManagedExecutor managedExecutor;
    private final ObjectMapper objectMapper;
    private final SnapshotStore snapshotStore;
    private final MeterRegistry meterRegistry;
    private final GraphHasher graphHasher;

    public TransformationExecutionService(GraphInstanceCache graphCache, ScriptEngineService scriptEngineService,
                                          TargetSystemWriterService targetSystemWriterService, ManagedExecutor managedExecutor,
                                          ObjectMapper objectMapper, SnapshotStore snapshotStore,
                                          MeterRegistry meterRegistry, GraphHasher graphHasher) {
        this.graphCache = graphCache;
        this.scriptEngineService = scriptEngineService;
        this.targetSystemWriterService = targetSystemWriterService;
        this.managedExecutor = managedExecutor;
        this.objectMapper = objectMapper;
        this.snapshotStore = snapshotStore;
        this.meterRegistry = meterRegistry;
        this.graphHasher = graphHasher;
    }

    /**
     * Asynchronously evaluates the logic graph and, if the condition passes,
     * executes the script transformation and all subsequent actions.
     *
     * @param payload An ExecutionPayload that contains TransformationData,
     *                GraphNodes and TransformationContext
     * @return A Uni that will eventually contain the TransformationResult, or null
     * if the pre-condition check failed.
     */
    public Uni<TransformationResult> execute(ExecutionPayload payload) {
        return evaluateLogicGraph(payload)
                .flatMap(conditionMet -> {
                    if (Boolean.TRUE.equals(conditionMet)) {
                        return executeMainTransformationFlow(payload);
                    } else {
                        logSkippedExecution(payload);
                        return Uni.createFrom().nullItem(); // Return null for skipped execution
                    }
                });
    }

    /**
     * Stage 1: Evaluates the logic graph to determine if the main transformation should run.
     * This is a blocking operation and is executed on a managed executor.
     */
    private Uni<Boolean> evaluateLogicGraph(ExecutionPayload payload) {
        return Uni.createFrom().item(() -> {
            try {
                MDC.put("transformationId", payload.job().transformationId().toString());
                Log.infof("Job %s: Evaluating pre-condition logic graph...", payload.job().jobId());

                List<Node> graphDefinition = payload.graphNodes();

                if (graphDefinition == null || graphDefinition.isEmpty()) {
                    Log.warnf("Job %s: No graph definition found in payload, defaulting to 'true'.", payload.job().jobId());
                    return true;
                }

                TypeReference<Map<String, JsonNode>> typeRef = new TypeReference<>() {
                };
                Map<String, JsonNode> dataContext = objectMapper.convertValue(payload.job().sourceData(), typeRef);
                String graphHash = graphHasher.hash(graphDefinition);

                StatefulLogicGraph graphInstance = graphCache.getOrCreate(
                        payload.job().transformationId(),
                        graphHash,
                        graphDefinition
                );

                return graphInstance.evaluate(dataContext);

            } catch (GraphEvaluationException e) {
                Log.errorf(e, "Job %s: Graph evaluation failed with error type %s: %s",
                        payload.job().jobId(), e.getErrorType(), e.getMessage());
                return false;
            } finally {
                MDC.remove("transformationId");
            }
        }).runSubscriptionOn(managedExecutor);
    }

    /**
     * Stage 2: Executes the full pipeline after the graph evaluation passes.
     * This includes script execution, metrics, snapshotting, and directive processing.
     */
    private Uni<TransformationResult> executeMainTransformationFlow(ExecutionPayload payload) {
        Log.infof("Job %s: Pre-condition PASSED. Proceeding to script transformation...", payload.job().jobId());

        Timer.Sample timerSample = Timer.start(meterRegistry);

        return scriptEngineService.transformAsync(payload.job())
                .invoke(transformationResult -> recordMetrics(payload, timerSample))
                .flatMap(transformationResult -> processScriptResult(transformationResult, payload));
    }

    /**
     * Stage 3: Processes the result from the script engine.
     * This involves snapshotting on failure and processing directives on success.
     */
    private Uni<TransformationResult> processScriptResult(TransformationResult result, ExecutionPayload payload) {
        // First, handle potential snapshotting for failed or errored executions.
        if (!result.isValidExecution() || result.getErrorInfo() != null) {
            storeFailedExecutionSnapshot(result, payload);
        }

        // Then, if the execution was valid, log output and process directives.
        if (result.isValidExecution()) {
            logScriptOutput(result);
            return targetSystemWriterService.processDirectives(result, payload.transformationContext())
                    .map(v -> result); // After directives are processed, return the original result.
        }

        // If execution was not valid, simply return the result as is.
        return Uni.createFrom().item(result);
    }

    private void recordMetrics(ExecutionPayload payload, Timer.Sample sample) {
        // Stop timer and record metric
        sample.stop(meterRegistry.timer(
                "script_execution_time_seconds",
                "scriptId", getScriptIdTag(payload),
                "transformationId", getTransformationIdTag(payload)
        ));

        // Record script load counter
        Counter.builder("script_execution_total")
                .description("Total number of script executions")
                .tag("scriptId", getScriptIdTag(payload))
                .tag("transformationId", getTransformationIdTag(payload))
                .register(meterRegistry)
                .increment();

        Log.infof("Job %s: Script execution completed and metrics recorded.", payload.job().jobId());
    }

    private void storeFailedExecutionSnapshot(TransformationResult result, ExecutionPayload payload) {
        try {
            // Populate the result with context needed for the snapshot
            result.setTransformationId(payload.transformationContext().id());
            result.setSourceData(payload.job().sourceData());

            var snapshot = SnapshotFactory.fromTransformationResult(result, objectMapper);
            snapshotStore.put(snapshot);
            Log.infof("Job %s: Stored snapshot id=%s for failed script execution.",
                    result.getJobId(), snapshot.getSnapshotId());
        } catch (Exception ex) {
            Log.error("Failed to create/store snapshot for failed execution", ex);
        }
    }

    private void logScriptOutput(TransformationResult result) {
        try {
            String outputJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(result.getOutputData());
            Log.infof(">>>> SCRIPT OUTPUT DATA (as JSON) for Job %s:\n%s", result.getJobId(), outputJson);
        } catch (JsonProcessingException e) {
            Log.errorf(e, "Job %s: Script output could not be serialized to JSON.", result.getJobId());
        }
    }

    private void logSkippedExecution(ExecutionPayload payload) {
        try {
            MDC.put("transformationId", payload.job().transformationId().toString());
            Log.infof("Job %s: Pre-condition FAILED. Skipping script transformation...", payload.job().jobId());
        } finally {
            MDC.remove("transformationId");
        }
    }

    private String getScriptIdTag(ExecutionPayload payload) {
        return payload.job().scriptId() != null ? payload.job().scriptId() : "unknown";
    }

    private String getTransformationIdTag(ExecutionPayload payload) {
        return payload.job().transformationId() != null ? payload.job().transformationId().toString() : "unknown";
    }
}