package de.unistuttgart.stayinsync.syncnode.syncjob;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.scriptengine.ScriptEngineService;
import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.syncnode.domain.ExecutionPayload;
import de.unistuttgart.stayinsync.syncnode.logic_engine.LogicGraphEvaluator;
import de.unistuttgart.stayinsync.transport.exception.GraphEvaluationException;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;

import java.util.Map;

@ApplicationScoped
public class TransformationExecutionService {

    @Inject
    LogicGraphEvaluator logicGraphEvaluator;

    @Inject
    ScriptEngineService scriptEngineService;

    @Inject
    TargetSystemWriterService targetSystemWriterService;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    ObjectMapper objectMapper;


    /**
     * Asynchronously evaluates the logic graph and, if the condition passes,
     * executes the script transformation.
     *
     * @param payload An ExecutionPayload that contains TransformationData, GraphNodes and TransformationContext
     * @return A Uni that will eventually contain the TransformationResult, or null if the
     * pre-condition check failed.
     */
    public Uni<TransformationResult> execute(ExecutionPayload payload) {
        return Uni.createFrom().item(() -> {
            Log.infof("Job %s: Evaluating pre-condition logic graph...", payload.job().jobId());
            TypeReference<Map<String, JsonNode>> typeRef = new TypeReference<>() {
            };
            Map<String, JsonNode> dataContext = objectMapper.convertValue(payload.job().sourceData(), typeRef);
            try {
                if (payload.graphNodes() == null || payload.graphNodes().size() == 0) {
                    return true;
                }
                return logicGraphEvaluator.evaluateGraph(payload.graphNodes(), dataContext);
            } catch (GraphEvaluationException e) {
                Log.errorf(e, "Job %s: Graph evaluation failed with error type %s: %s",
                        payload.job().jobId(), e.getErrorType(), e.getMessage());
                return false;
            }
        }).runSubscriptionOn(managedExecutor).onItem().transformToUni(conditionMet -> {
            if (conditionMet) {
                Log.infof("Job %s: Pre-condition PASSED. Proceeding to script transformation...", payload.job().jobId());
                return scriptEngineService.transformAsync(payload.job())
                        .onItem().transformToUni(transformationResult -> {
                            if (transformationResult != null && transformationResult.isValidExecution()) {
                                try {
                                    String outputJson = objectMapper.writerWithDefaultPrettyPrinter()
                                            .writeValueAsString(transformationResult.getOutputData());
                                    Log.infof(">>>> SCRIPT OUTPUT DATA (as JSON):\n%s", outputJson);
                                } catch (JsonProcessingException e) {
                                    Log.error("Script output could not be serialized to JSON", e);
                                }

                                return targetSystemWriterService.processDirectives(transformationResult, payload.transformationContext())
                                        .map(v -> transformationResult);
                            }
                            return Uni.createFrom().item(transformationResult);
                        });
            } else {
                Log.infof("Job %s: Pre-condition FAILED. Skipping script transformation...", payload.job().jobId());
                return Uni.createFrom().nullItem();
            }
        });
    }
}
