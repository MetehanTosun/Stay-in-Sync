package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;


import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.util.GraphTopologicalSorter;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;

/**
 * Provides a service to validate the structural and logical integrity of a graph.
 */
@ApplicationScoped
public class GraphValidatorService {

    @Inject
    GraphTopologicalSorter sorter;

    /**
     * Performs a comprehensive validation of a graph before it is persisted.
     * <p>
     * This method checks for:
     * <ol>
     * <li>Correct configuration for each individual LogicNode's operator.</li>
     * <li>The absence of cycles (ensuring it is a DAG).</li>
     * <li>The existence of exactly one terminal (target) node.</li>
     * <li>The terminal node being a LogicNode that returns a Boolean value.</li>
     * </ol>
     *
     * @param graphNodes The complete list of nodes in the graph.
     * @throws IllegalArgumentException if the graph violates any of the validation rules.
     */
    public void validateGraph(List<Node> graphNodes) {
        if (graphNodes == null || graphNodes.isEmpty()) {
            throw new IllegalArgumentException("Graph node list cannot be null or empty.");
        }

        // 1. Perform operator-specific validation on each LogicNode.
        validateNodeOperators(graphNodes);

        // 2. Perform topological sort to detect cycles.
        GraphTopologicalSorter.SortResult sortResult = sorter.sort(graphNodes);
        if (sortResult.hasCycle()) {
            throw new IllegalArgumentException("Graph validation failed: The graph contains a cycle.");
        }

        // 3. Find all terminal nodes (nodes with no children).
        List<Node> targetNodes = findTargetNodes(graphNodes);

        if (targetNodes.size() != 1) {
            throw new IllegalArgumentException("Graph validation failed: Exactly one target node is required, but found " + targetNodes.size());
        }

        // 4. Validate the single target node.
        Node finalTargetNode = targetNodes.get(0);
        if (!(finalTargetNode instanceof LogicNode)) {
            throw new IllegalArgumentException("Graph validation failed: The target node must be a LogicNode.");
        }

        // 5. Ensure the target node's operator returns a Boolean.
        LogicNode finalLogicNode = (LogicNode) finalTargetNode;
        Operation finalOperation = finalLogicNode.getOperator().getOperationStrategy();
        if (!Boolean.class.isAssignableFrom(finalOperation.getReturnType())) {
            throw new IllegalArgumentException(
                    "Graph validation failed: The operator '" + finalLogicNode.getOperator().name() +
                            "' of the target node must return a Boolean, but returns '" +
                            finalOperation.getReturnType().getSimpleName() + "'."
            );
        }
    }

    /**
     * Iterates through all nodes and validates the operator configuration for each LogicNode.
     */
    private void validateNodeOperators(List<Node> graphNodes) {
        for (Node node : graphNodes) {
            if (node instanceof LogicNode) {
                LogicNode logicNode = (LogicNode) node;
                try {
                    Operation strategy = logicNode.getOperator().getOperationStrategy();
                    strategy.validateNode(logicNode);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Graph validation failed for node '" + logicNode.getName() + "': " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Finds all nodes in the graph that have no outgoing edges (children).
     */
    private List<Node> findTargetNodes(List<Node> graphNodes) {
        // 1. Create a Set to efficiently store all nodes that are used as inputs (parents).
        Set<Node> parentNodes = new HashSet<>();

        // 2. First loop: Iterate through all nodes to find their inputs.
        for (Node node : graphNodes) {
            if (node.getInputNodes() != null) {
                // 3. Second (nested) loop: Add each input node to the set of parents.
                for (Node parent : node.getInputNodes()) {
                    if (parent != null) {
                        parentNodes.add(parent);
                    }
                }
            }
        }

        // 4. Create a list to store the final target nodes.
        List<Node> targetNodes = new ArrayList<>();

        // 5. Third loop: Iterate through all nodes again.
        for (Node node : graphNodes) {
            // A node is a target if it is NOT in our set of parent nodes.
            if (!parentNodes.contains(node)) {
                targetNodes.add(node);
            }
        }

        return targetNodes;
    }
}