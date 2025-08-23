package de.unistuttgart.stayinsync.syncnode.syncjob;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.scriptengine.ScriptEngineService;
import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.syncnode.domain.TransformJob;
import de.unistuttgart.stayinsync.syncnode.logic_engine.LogicGraphEvaluator;
import de.unistuttgart.stayinsync.syncnode.logic_engine.LogicGraphEvaluator.EvaluationResult;
import de.unistuttgart.stayinsync.syncnode.logic_engine.SnapshotCacheService;
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

    @Inject
    LogicGraphEvaluator logicGraphEvaluator;
    @Inject
    ScriptEngineService scriptEngineService;
    @Inject
    ManagedExecutor managedExecutor;
    @Inject
    ObjectMapper objectMapper;
    @Inject
    SnapshotCacheService snapshotCache;

    public Uni<TransformationResult> execute(TransformJob job, List<Node> graphNodes) {
        Uni<EvaluationResult> evaluationUni = Uni.createFrom().item(() -> {
            Log.infof("Job %s: Evaluating pre-condition logic graph...", job.jobId());
            long transformationId = Long.parseLong(job.jobId());
            JsonNode oldSnapshot = snapshotCache.getSnapshot(transformationId)
                    .orElse(objectMapper.createObjectNode());

            TypeReference<Map<String, JsonNode>> typeRef = new TypeReference<>() {};
            Map<String, JsonNode> dataContext = objectMapper.convertValue(job.sourceData(), typeRef);
            dataContext.put("__snapshot", oldSnapshot);

            try {
                return logicGraphEvaluator.evaluateGraph(graphNodes, dataContext, job);
            } catch (GraphEvaluationException e) {
                Log.errorf(e, "Job %s: Graph evaluation failed...", job.jobId());
                return new EvaluationResult(false, null);
            }
        }).runSubscriptionOn(managedExecutor);

        return evaluationUni.onItem().transformToUni(conditionMet -> {
            if (conditionMet.newSnapshot() != null) {
                long transformationId = Long.parseLong(job.jobId());
                JsonNode newSnapshotNode = objectMapper.valueToTree(conditionMet.newSnapshot());
                snapshotCache.saveSnapshot(transformationId, newSnapshotNode);
                Log.infof("Job %s: Saving new snapshot.", job.jobId());
            }

            if (conditionMet.finalResult()) {
                Log.infof("Job %s: Pre-condition PASSED. Proceeding to script transformation...", job.jobId());
                return scriptEngineService.transformAsync(job);
            } else {
                Log.infof("Job %s: Pre-condition FAILED. Skipping script transformation...", job.jobId());
                return Uni.createFrom().nullItem();
            }
        });
    }
}