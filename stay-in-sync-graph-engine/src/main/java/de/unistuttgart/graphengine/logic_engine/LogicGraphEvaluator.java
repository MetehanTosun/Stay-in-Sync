package de.unistuttgart.graphengine.logic_engine;

import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.graphengine.service.GraphTopologicalSorter;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;

/**
 * A stateless utility service to evaluate a logic graph composed of Node objects.
 * This evaluator is responsible for executing the nodes of a given graph in the
 * correct topological order and returning the final boolean result. It does not
 * manage state (like snapshots) itself, making it a reusable component.
 */
@ApplicationScoped
public class LogicGraphEvaluator {

    private final GraphTopologicalSorter sorter = new GraphTopologicalSorter();

    /**
     * Evaluates a given valid, directed acyclic graph (DAG) of {@link Node}s.
     * <p>
     * The method executes all nodes in a topologically sorted order and extracts the
     * final boolean result from the graph's last node in the sorted list (which is
     * assumed to be the {@code FinalNode}). It is the responsibility of the caller to manage
     * any state, such as snapshots, within the provided dataContext.
     *
     * @param allNodesInGraph A non-empty list containing all nodes that constitute the graph.
     * @param dataContext     A map containing runtime data. It is a flexible container that can hold
     * various types of data required by the nodes, such as the source data
     * (e.g., a {@code Map<String, JsonNode>}) and snapshots (e.g., a {@code Map<String, SnapshotEntry>}).
     * @return The final {@code boolean} result of the graph evaluation.
     * @throws GraphEvaluationException if the provided list of nodes is null or empty, or if any
     * unexpected runtime error occurs during the evaluation process.
     */
    public boolean evaluateGraph(List<Node> allNodesInGraph, Map<String, Object> dataContext) throws GraphEvaluationException {

        if (allNodesInGraph == null || allNodesInGraph.isEmpty()) {
            Log.warn("Attempted to evaluate a null or empty graph. Throwing GraphEvaluationException.");
            throw new GraphEvaluationException(
                    GraphEvaluationException.ErrorType.INVALID_INPUT,
                    "Invalid Input",
                    "The list of graph nodes to evaluate cannot be null or empty.",
                    null);
        }
        Log.debugf("Starting evaluation of graph with %d nodes and data context keys: %s", allNodesInGraph.size(),
                dataContext.keySet());

        try {
            // Reset results on all nodes before evaluation.
            Log.debug("Resetting calculated results on all nodes before evaluation.");
            for (Node node : allNodesInGraph) {
                node.setCalculatedResult(null);
            }

            // 1. Get the topologically sorted list of nodes.
            Log.debug("Topologically sorting the graph to determine evaluation order.");
            List<Node> sortedNodes = sorter.sort(allNodesInGraph).sortedNodes();
            Log.debugf("Graph sorted. Evaluation order contains %d nodes.", sortedNodes.size());

            // 2. Evaluate each node in the correct order.
            Log.debug("Evaluating each node in topological order...");
            for (Node node : sortedNodes) {
                Log.tracef("Calculating node ID %d ('%s')...", node.getId(), node.getName());
                // Each node can now access the flexible dataContext and cast what it needs.
                node.calculate(dataContext);
                Log.tracef("...Node ID %d result: %s", node.getId(), node.getCalculatedResult());
            }

            // 3. Get the final boolean result from the last node in the sorted list (the FinalNode).
            Node finalTargetNode = sortedNodes.get(sortedNodes.size() - 1);
            boolean finalResult = (boolean) finalTargetNode.getCalculatedResult();
            Log.infof("Graph evaluation completed successfully. Final result: %b", finalResult);

            // 4. Return the simple boolean result. The caller (e.g., StatefulLogicGraph)
            // is responsible for handling the snapshot.
            return finalResult;

        } catch (Exception e) {
            Log.errorf(e, "An unexpected error occurred during graph evaluation.");
            throw new GraphEvaluationException(
                    GraphEvaluationException.ErrorType.EXECUTION_FAILED,
                    "Evaluation Failed",
                    "An unexpected error occurred during graph evaluation.",
                    e);
        }
    }
}