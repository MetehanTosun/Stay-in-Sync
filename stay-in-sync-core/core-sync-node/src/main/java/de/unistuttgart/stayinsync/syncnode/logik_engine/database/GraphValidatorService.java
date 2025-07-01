package de.unistuttgart.stayinsync.syncnode.logik_engine.database;

import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class GraphValidatorService {

    /**
     * Validates the structural integrity of a complete logic graph.
     * It iterates through all nodes and applies the specific validation logic
     * of each node's operator.
     *
     * @param graphNodes The complete list of nodes in the graph.
     * @throws IllegalArgumentException if any node in the graph is configured incorrectly.
     */
    public void validateGraph(List<LogicNode> graphNodes) {
        if (graphNodes == null || graphNodes.isEmpty()) {
            throw new IllegalArgumentException("Graph cannot be null or empty.");
        }

        for (LogicNode node : graphNodes) {
            try {
                // Get the strategy and delegate the validation call.
                Operation strategy = node.getOperator().getOperationStrategy();
                strategy.validate(node);
            } catch (IllegalArgumentException e) {
                // Re-throw the exception with more context.
                throw new IllegalArgumentException(
                        "Graph validation failed for node '" + node.getNodeName() + "': " + e.getMessage(), e
                );
            }
        }
        // If we get here, the graph is structurally valid.
    }
}
