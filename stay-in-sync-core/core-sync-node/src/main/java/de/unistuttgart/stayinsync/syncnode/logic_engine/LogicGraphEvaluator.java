package de.unistuttgart.stayinsync.syncnode.logic_engine;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.util.GraphTopologicalSorter;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
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
     *
     * @param allNodesInGraph A non-empty list containing all {@link Node}s that constitute the graph.
     * @param dataContext     A map containing the runtime data sources required by ProviderNodes.
     * @return {@code true} or {@code false} representing the final evaluated state of the graph.
     * @throws IllegalArgumentException if the provided list of nodes is null or empty.
     */
    //Todo graph can only be evaluated, if flag (finalized) is true
    public boolean evaluateGraph(List<Node> allNodesInGraph, Map<String, JsonNode> dataContext) {
        if (allNodesInGraph == null || allNodesInGraph.isEmpty()) {
            throw new IllegalArgumentException("The list of graph nodes cannot be null or empty.");
        }

        // Reset results before evaluation
        for (Node node : allNodesInGraph) {
            node.setCalculatedResult(null);
        }

        // 1. Get the topologically sorted list of nodes.
        // We assume the graph is valid and has no cycles, as it was validated upon saving.
        List<Node> sortedNodes = sorter.sort(allNodesInGraph).sortedNodes();

        // 2. Evaluate each node in the correct order.
        for (Node node : sortedNodes) {
            node.calculate(dataContext);
        }

        // 3. The final result is the calculated value of the last node in the sorted list.
        Node finalTargetNode = sortedNodes.get(sortedNodes.size() - 1);
        return (boolean) finalTargetNode.getCalculatedResult();
    }
}