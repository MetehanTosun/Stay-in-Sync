package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;


import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.util.GraphTopologicalSorter;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.ValidationResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides a service to validate the structural and logical integrity of a graph.
 */
@ApplicationScoped
public class GraphValidatorService {

    @Inject
    GraphTopologicalSorter sorter;

    /**
     * Performs a comprehensive validation of a graph.
     * <p>
     * This method runs all structural and logical checks but does not throw an exception.
     * Instead, it returns a detailed result object containing the validation status
     * and a list of all found errors.
     *
     * @param graphNodes The complete list of nodes in the graph.
     * @return A {@link ValidationResult} object summarizing the outcome.
     */
    public ValidationResult validateGraph(List<Node> graphNodes) {
        List<String> errors = new ArrayList<>();

        if (graphNodes == null || graphNodes.isEmpty()) {
            errors.add("Graph node list cannot be null or empty.");
            return new ValidationResult(false, errors);
        }

        // 1. Perform operator-specific validation on each LogicNode.
        validateNodeOperators(graphNodes, errors);

        // 2. Perform topological sort to detect cycles.
        GraphTopologicalSorter.SortResult sortResult = sorter.sort(graphNodes);
        if (sortResult.hasCycle()) {
            errors.add("The graph contains a cycle.");
        }

        // 3. Find all terminal nodes (nodes with no children).
        List<Node> targetNodes = findTargetNodes(graphNodes);

        if (targetNodes.size() != 1) {
            errors.add("Exactly one target node is required, but found " + targetNodes.size());
        } else {
            // If there is exactly one target node, perform further checks on it.
            Node finalTargetNode = targetNodes.get(0);
            if (!(finalTargetNode instanceof LogicNode)) {
                errors.add("The target node must be a LogicNode.");
            } else {
                // Ensure the target node's operator returns a Boolean.
                LogicNode finalLogicNode = (LogicNode) finalTargetNode;
                Operation finalOperation = finalLogicNode.getOperator().getOperationStrategy();
                if (!Boolean.class.isAssignableFrom(finalOperation.getReturnType())) {
                    errors.add(
                            "The operator '" + finalLogicNode.getOperator().name() +
                                    "' of the target node must return a Boolean, but returns '" +
                                    finalOperation.getReturnType().getSimpleName() + "'."
                    );
                }
            }
        }

        // The graph is valid if and only if the list of errors is empty.
        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Iterates through all nodes and validates the operator configuration for each LogicNode,
     * collecting any error messages.
     */
    private void validateNodeOperators(List<Node> graphNodes, List<String> errors) {
        for (Node node : graphNodes) {
            if (node instanceof LogicNode) {
                LogicNode logicNode = (LogicNode) node;
                try {
                    Operation strategy = logicNode.getOperator().getOperationStrategy();
                    strategy.validateNode(logicNode);
                } catch (IllegalArgumentException e) {
                    // Add the specific error message from the operator to our list.
                    errors.add("Node '" + logicNode.getName() + "': " + e.getMessage());
                }
            }
        }
    }

    /**
     * Finds all nodes in the graph that have no outgoing edges (children).
     */
    private List<Node> findTargetNodes(List<Node> graphNodes) {
        Set<Node> parentNodes = graphNodes.stream()
                .filter(node -> node.getInputNodes() != null)
                .flatMap(node -> node.getInputNodes().stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Node> targetNodes = new ArrayList<>();
        for (Node node : graphNodes) {
            if (!parentNodes.contains(node)) {
                targetNodes.add(node);
            }
        }
        return targetNodes;
    }
}