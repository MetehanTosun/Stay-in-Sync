package de.unistuttgart.stayinsync.syncnode.logic_engine;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.GraphEvaluationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

/**
 * Evaluates a logic graph composed of Node objects.
 */
@ApplicationScoped
public class LogicGraphEvaluator {

    @Inject
    GraphTopologicalSorter sorter;

    /**
     * Evaluates a given valid, directed acyclic graph (DAG) of {@link Node}s.
     * It assumes the graph has already been validated and contains a single FinalNode.
     *
     * @param allNodesInGraph A non-empty list containing all nodes that constitute the graph.
     * @param dataContext     A map containing the runtime data sources required by ProviderNodes.
     * @return {@code true} or {@code false} representing the final evaluated state of the graph.
     * @throws GraphEvaluationException if the provided list of nodes is null or empty, or if any
     * unexpected runtime error occurs during the evaluation process.
     */
    public boolean evaluateGraph(List<Node> allNodesInGraph, Map<String, JsonNode> dataContext) throws GraphEvaluationException{
        if (allNodesInGraph == null || allNodesInGraph.isEmpty()) {
            Log.warn("Attempted to evaluate a null or empty graph. Throwing GraphEvaluationException.");
            throw new GraphEvaluationException(
                    GraphEvaluationException.ErrorType.INVALID_INPUT,
                    "Invalid Input",
                    "The list of graph nodes to evaluate cannot be null or empty.",
                    null
            );
        }
        Log.debugf("Starting evaluation of graph with %d nodes and data context keys: %s", allNodesInGraph.size(), dataContext.keySet());

        try {
            // Reset results before evaluation
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
                node.calculate(dataContext);
                Log.tracef("...Node ID %d result: %s", node.getId(), node.getCalculatedResult());
            }

            // 3. The final result is the calculated value of the last node in the sorted list.
            Node finalTargetNode = sortedNodes.get(sortedNodes.size() - 1);
            Log.debugf("Retrieving final result from target node with ID %d ('%s').", finalTargetNode.getId(), finalTargetNode.getName());

            boolean finalResult = (boolean) finalTargetNode.getCalculatedResult();
            Log.infof("Graph evaluation completed successfully. Final result: %b", finalResult);

            return finalResult;
        } catch (Exception e) {
            Log.errorf(e, "An unexpected error occurred during graph evaluation. Wrapping in GraphEvaluationException.");
            throw new GraphEvaluationException(
                    GraphEvaluationException.ErrorType.EXECUTION_FAILED,
                    "Evaluation Failed",
                    "An unexpected error occurred during graph evaluation.",
                    e
            );
        }
    }
}