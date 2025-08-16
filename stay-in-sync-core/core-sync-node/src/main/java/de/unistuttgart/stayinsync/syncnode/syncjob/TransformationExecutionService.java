package de.unistuttgart.stayinsync.syncnode.syncjob;

import java.util.List;
import java.util.Map;

import de.unistuttgart.stayinsync.syncnode.logic_engine.StateServiceClient;
import org.eclipse.microprofile.context.ManagedExecutor;
// HINZUGEFÜGT: Import für den REST-Client (Annahme)
// import de.unistuttgart.stayinsync.syncnode.rest.StateServiceClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unistuttgart.stayinsync.scriptengine.ScriptEngineService;
import de.unistuttgart.stayinsync.scriptengine.message.TransformationResult;
import de.unistuttgart.stayinsync.syncnode.domain.TransformJob;
import de.unistuttgart.stayinsync.syncnode.logic_engine.LogicGraphEvaluator;
import de.unistuttgart.stayinsync.syncnode.logic_engine.LogicGraphEvaluator.EvaluationResult; // HINZUGEFÜGT
import de.unistuttgart.stayinsync.transport.exception.GraphEvaluationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class TransformationExecutionService {

    // KORREKTUR: Fehlende @Inject Annotation
    @Inject
    LogicGraphEvaluator logicGraphEvaluator;

    @Inject
    ScriptEngineService scriptEngineService;

    @Inject
    ManagedExecutor managedExecutor;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @RestClient
    StateServiceClient stateServiceClient;


    /**
     * Asynchronously evaluates the logic graph, saves the new snapshot, and if the condition passes,
     * executes the script transformation.
     *
     * @param job        The TransformJob containing the source data.
     * @param graphNodes The list of nodes for the logic graph evaluation.
     * @return A Uni that will eventually contain the TransformationResult, or null
     * if the
     * pre-condition check failed.
     */
    public Uni<TransformationResult> execute(TransformJob job, List<Node> graphNodes) {
        return Uni.createFrom().item(() -> {
                    Log.infof("Job %s: Evaluating pre-condition logic graph...", job.jobId());
                    TypeReference<Map<String, JsonNode>> typeRef = new TypeReference<>() {};
                    Map<String, JsonNode> dataContext = objectMapper.convertValue(job.sourceData(), typeRef);
                    try {
                        return logicGraphEvaluator.evaluateGraph(graphNodes, dataContext, job);
                    } catch (GraphEvaluationException e) {
                        Log.errorf(e, "Job %s: Graph evaluation failed with error type %s: %s",
                                job.jobId(), e.getErrorType(), e.getMessage());
                        return new EvaluationResult(false, null);
                    }
                })
                .runSubscriptionOn(managedExecutor)
                .onItem().invoke(result -> {
                    if (result.newSnapshot() != null) {
                        Log.infof("Job %s: Saving new snapshot...", job.jobId());
                        try {
                            long transformationId = Long.parseLong(job.name().replace("Transformation-", ""));
                            String newSnapshotJson = objectMapper.writeValueAsString(result.newSnapshot());
                            stateServiceClient.saveSnapshot(transformationId, newSnapshotJson);
                            Log.debugf("Job %s: Successfully saved snapshot.", job.jobId());
                        } catch (Exception e) {
                            Log.errorf(e, "Job %s: CRITICAL - Failed to save new snapshot!", job.jobId());
                        }
                    }
                })
                .onItem().transformToUni(result -> {
                    if (result.finalResult()) {
                        Log.infof("Job %s: Pre-condition PASSED. Proceeding to script transformation...", job.jobId());
                        return scriptEngineService.transformAsync(job);
                    } else {
                        Log.infof("Job %s: Pre-condition FAILED. Skipping script transformation...", job.jobId());
                        return Uni.createFrom().nullItem();
                    }
                });
    }
}