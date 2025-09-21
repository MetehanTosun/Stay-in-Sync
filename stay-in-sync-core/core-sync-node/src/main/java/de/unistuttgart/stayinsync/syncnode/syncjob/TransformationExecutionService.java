package de.unistuttgart.stayinsync.syncnode.syncjob;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.nodes.Node; // Wichtiger Import
import de.unistuttgart.stayinsync.scriptengine.ScriptEngineService;
import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
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
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.MDC;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TransformationExecutionService {

    @Inject
    GraphInstanceCache graphCache;

    @Inject
    ScriptEngineService scriptEngineService;

    @Inject
    TargetSystemWriterService targetSystemWriterService;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    SnapshotStore snapshotStore;

    @Inject
    MeterRegistry meterRegistry;

    /**
     * Asynchronously evaluates the logic graph and, if the condition passes,
     * executes the script transformation.
     *
     * @param payload An ExecutionPayload that contains TransformationData,
     *                GraphNodes and TransformationContext
     * @return A Uni that will eventually contain the TransformationResult, or null
     *         if the pre-condition check failed.
     */
    public Uni<TransformationResult> execute(ExecutionPayload payload) {
        return Uni.createFrom().item(() -> {
            try {
                MDC.put("transformationId", payload.job().transformationId().toString());
                Log.infof("Job %s: Evaluating pre-condition logic graph...", payload.job().jobId());

                TypeReference<Map<String, JsonNode>> typeRef = new TypeReference<>() {};
                Map<String, JsonNode> dataConext = objectMapper.convertValue(payload.job().sourceData(), typeRef);

                List<Node> graphDefinition = payload.graphNodes();

                if (graphDefinition == null || graphDefinition.isEmpty()) {
                    Log.warnf("Job %s: No graph definition found in payload, defaulting to 'true'.", payload.job().jobId());
                    return true;
                }

                StatefulLogicGraph graphInstance = graphCache.getOrCreate(
                        payload.job().transformationId(),
                        graphDefinition
                );

                return graphInstance.evaluate(dataConext);

            } catch (GraphEvaluationException e) {
                Log.errorf(e, "Job %s: Graph evaluation failed with error type %s: %s",
                        payload.job().jobId(), e.getErrorType(), e.getMessage());
                return false;
            } finally {
                MDC.remove("transformationId");
            }
        }).runSubscriptionOn(managedExecutor).onItem().transformToUni(conditionMet -> {
            if (conditionMet) {
                Log.infof("Job %s: Pre-condition PASSED. Proceeding to script transformation...",
                        payload.job().jobId());

                // Start timer for script execution
                Timer.Sample sample = Timer.start(meterRegistry);

                return scriptEngineService.transformAsync(payload.job())
                        .invoke(transformationResult -> {
                            // Stop timer and record metric with transformationId instead of jobId
                            sample.stop(
                                    meterRegistry.timer(
                                            "script_execution_time_seconds",
                                            "scriptId", payload.job().scriptId() != null ? payload.job().scriptId() : "unknown",
                                            "transformationId", payload.job().transformationId() != null ? payload.job().transformationId().toString() : "unknown"
                                    )
                            );

                            // Record script load (count how many times a script runs)
                            Counter.builder("script_execution_total")
                                    .description("Total number of script executions")
                                    .tag("scriptId", payload.job().scriptId() != null ? payload.job().scriptId() : "unknown")
                                    .tag("transformationId", payload.job().transformationId() != null ? payload.job().transformationId().toString() : "unknown")
                                    .register(meterRegistry)
                                    .increment();

                            Log.infof("Job %s: Script execution completed.", payload.job().jobId());
                        })
                        .onItem().transformToUni(transformationResult -> {

                            // CASE (Failure): If the transformation reported failure → snapshot
                            if (!transformationResult.isValidExecution()
                                    || transformationResult.getErrorInfo() != null) {
                                try {
                                    transformationResult.setTransformationId(payload.transformationContext().id());
                                    transformationResult.setSourceData(payload.job().sourceData());

                                    var snapshot = SnapshotFactory.fromTransformationResult(transformationResult, objectMapper);
                                    snapshotStore.put(snapshot);
                                    Log.infof("Job %s: Stored snapshot id=%s for failed script execution.",
                                            transformationResult.getJobId(), snapshot.getSnapshotId());
                                } catch (Exception ex) {
                                    Log.error("Failed to create/store snapshot for failed execution", ex);
                                }
                            }

                            if (transformationResult != null && transformationResult.isValidExecution()) {
                                try {
                                    String outputJson = objectMapper.writerWithDefaultPrettyPrinter()
                                            .writeValueAsString(transformationResult.getOutputData());
                                    Log.infof(">>>> SCRIPT OUTPUT DATA (as JSON):\n%s", outputJson);
                                } catch (JsonProcessingException e) {
                                    Log.error("Script output could not be serialized to JSON", e);
                                }
                                return targetSystemWriterService
                                        .processDirectives(transformationResult, payload.transformationContext())
                                        .map(v -> transformationResult);
                            }
                            return Uni.createFrom().item(transformationResult);
                        });

            } else {
                try {
                    MDC.put("transformationId", payload.job().transformationId().toString());
                    Log.infof("Job %s: Pre-condition FAILED. Skipping script transformation...", payload.job().jobId());
                } finally {
                    MDC.remove("transformationId");
                }
                return Uni.createFrom().nullItem();
            }
        });
    }
}