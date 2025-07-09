package de.unistuttgart.stayinsync.syncnode.logik_engine.database;

import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class GraphValidatorService {

    /**
     * Validates a complete logic graph.
     * It iterates through all nodes and, for each LogicNode, applies the specific
     * validation logic of its operator to ensure it is correctly configured.
     *
     * @param graphNodes The complete list of nodes in the graph.
     * @throws IllegalArgumentException if any node in the graph is configured incorrectly.
     */
    public void validateGraph(List<Node> graphNodes) {
        if (graphNodes == null || graphNodes.isEmpty()) {
            throw new IllegalArgumentException("Graph node list cannot be null or empty.");
        }

        // Iterate over all nodes in the graph.
        for (Node node : graphNodes) {
            // The validation logic only applies to LogicNodes, as they are the only
            // ones with operators that have input constraints.
            if (node instanceof LogicNode) {
                LogicNode logicNode = (LogicNode) node;
                try {
                    // Get the strategy and delegate the validation call.
                    Operation strategy = logicNode.getOperator().getOperationStrategy();
                    strategy.validate(logicNode);
                } catch (IllegalArgumentException e) {
                    // Re-throw the exception with more context.
                    throw new IllegalArgumentException(
                            "Graph validation failed for node '" + logicNode.getName() + "': " + e.getMessage(), e
                    );
                }
            }
        }
        // If we get here, the graph is structurally valid.
    }
}
