package de.unistuttgart.stayinsync.syncnode.logik_engine.nodes;

import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.LogicOperator;
import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.Operation;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.inputNodes.InputNode;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Arrays;

@ApplicationScoped
public class CreateLogicNode {

    /**
     * Creates and validates a new LogicNode. This is the preferred way to create nodes
     * to ensure immediate validation of the input configuration.
     *
     * @param nodeName  A unique name for the node.
     * @param operator  The operator the node will use.
     * @param providers The input providers for this node.
     * @return A validated, ready-to-use LogicNode.
     * @throws IllegalArgumentException if the provided inputs are not valid for the given operator.
     */
    public LogicNode createNode(String nodeName, LogicOperator operator, InputNode... providers) {
        // Step 1: Create the "dumb" LogicNode with the constructor.
        LogicNode node = new LogicNode(nodeName, operator);
        node.setInputProviders(Arrays.asList(providers));

        // Step 2: Run the validation IMMEDIATELY.
        Operation strategy = operator.getOperationStrategy();
        try {
            strategy.validate(node);
        } catch (IllegalArgumentException e) {
            // We catch the exception and enrich it with more context.
            throw new IllegalArgumentException(
                    "Failed to create node '" + nodeName + "' with operator '" + operator.name() + "'. " +
                            "Validation error: " + e.getMessage(), e
            );
        }
        return node;
    }
}
