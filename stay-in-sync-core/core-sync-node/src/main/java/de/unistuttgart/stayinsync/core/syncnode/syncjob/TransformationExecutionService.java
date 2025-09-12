package de.unistuttgart.stayinsync.core.syncnode.syncjob;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.logic_engine.LogicGraphEvaluator;
import de.unistuttgart.graphengine.logic_engine.SnapshotCacheService;
import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.stayinsync.core.scriptengine.ScriptEngineService;
import de.unistuttgart.stayinsync.core.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.core.syncnode.domain.TransformJob;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TransformationExecutionService {


    LogicGraphEvaluator logicGraphEvaluator = new LogicGraphEvaluator();
    @Inject
    ScriptEngineService scriptEngineService;
    @Inject
    ManagedExecutor managedExecutor;
    @Inject
    ObjectMapper objectMapper;

    SnapshotCacheService snapshotCache = new SnapshotCacheService();

    public Uni<TransformationResult> execute(TransformJob job, List<Node> graphNodes) {
        Uni<LogicGraphEvaluator.EvaluationResult> evaluationUni = Uni.createFrom().item(() -> {
            Log.infof("Job %s: Evaluating pre-condition logic graph...", job.jobId());
            long transformationId = Long.parseLong(job.jobId());
            JsonNode oldSnapshot = snapshotCache.getSnapshot(transformationId)
                    .orElse(objectMapper.createObjectNode());

            TypeReference<Map<String, JsonNode>> typeRef = new TypeReference<>() {};
            Map<String, JsonNode> dataContext = objectMapper.convertValue(job.sourceData(), typeRef);
            dataContext.put("__snapshot", oldSnapshot);

            try {
                return logicGraphEvaluator.evaluateGraph(graphNodes, dataContext);
            } catch (GraphEvaluationException e) {
                Log.errorf(e, "Job %s: Graph evaluation failed...", job.jobId());
                return new LogicGraphEvaluator.EvaluationResult(false, null);
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