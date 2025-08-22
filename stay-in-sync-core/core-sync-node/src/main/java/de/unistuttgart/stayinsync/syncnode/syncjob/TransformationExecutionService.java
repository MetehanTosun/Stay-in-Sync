package de.unistuttgart.stayinsync.syncnode.syncjob;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.scriptengine.ScriptEngineService;
import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.syncnode.domain.TransformJob;
import de.unistuttgart.stayinsync.syncnode.logic_engine.LogicGraphEvaluator;
import de.unistuttgart.stayinsync.transport.exception.GraphEvaluationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TransformationExecutionService {

    LogicGraphEvaluator logicGraphEvaluator;

    @Inject
    ScriptEngineService scriptEngineService;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    ObjectMapper objectMapper;


    /**
     * Asynchronously evaluates the logic graph and, if the condition passes,
     * executes the script transformation.
     *
     * @param job         The TransformJob containing the source data.
     * @param graphNodes  The list of nodes for the logic graph evaluation.
     * @return A Uni that will eventually contain the TransformationResult, or null if the
     *         pre-condition check failed.
     */
    public Uni<TransformationResult> execute(TransformJob job, List<Node> graphNodes){
        return Uni.createFrom().item(()-> {
            Log.infof("Job %s: Evaluating pre-condition logic graph...", job.jobId());
            TypeReference<Map<String, JsonNode>> typeRef = new TypeReference<>() {};
            Map<String, JsonNode> dataContext = objectMapper.convertValue(job.sourceData(), typeRef);
            try {
                return logicGraphEvaluator.evaluateGraph(graphNodes, dataContext);
            } catch (GraphEvaluationException e) {
                Log.errorf(e, "Job %s: Graph evaluation failed with error type %s: %s",
                        job.jobId(), e.getErrorType(), e.getMessage());
                return false;
            }
        }).runSubscriptionOn(managedExecutor).onItem().transformToUni(conditionMet -> {
            if (conditionMet) {
                Log.infof("Job %s: Pre-condition PASSED. Proceeding to script transformation...", job.jobId());
                return scriptEngineService.transformAsync(job);
            } else {
                Log.infof("Job %s: Pre-condition FAILED. Skipping script transformation...", job.jobId());
                return Uni.createFrom().nullItem();
            }
        });
    }
}
