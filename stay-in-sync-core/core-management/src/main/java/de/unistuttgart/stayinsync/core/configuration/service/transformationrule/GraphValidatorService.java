package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;


import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.util.GraphTopologicalSorter;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.validation_error.*;
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
     * This method runs all structural and logical checks and returns a list of all
     * found validation errors. An empty list signifies a valid graph.
     *
     * @param graphNodes The complete list of nodes in the graph.
     * @return A {@code List<ValidationError>} containing structured error objects.
     */
    public List<ValidationError> validateGraph(List<Node> graphNodes) {
        List<ValidationError> errors = new ArrayList<>();

        if (graphNodes == null || graphNodes.isEmpty()) {
            // Here we could create a new ValidationError type if needed, e.g., new GraphIsEmptyError()
            errors.add(new FinalNodeError("Graph node list cannot be null or empty."));
            return errors;
        }

        // 1. Perform operator-specific validation on each LogicNode.
        validateNodeOperators(graphNodes, errors);

        // 2. Perform topological sort to detect cycles.
        GraphTopologicalSorter.SortResult sortResult = sorter.sort(graphNodes);
        if (sortResult.hasCycle()) {
            // Use the list of cycle nodes from the sort result to create a detailed error.
            errors.add(new CycleError(sortResult.cycleNodeIds()));
        }

        // 3. Find all terminal nodes (nodes with no children).
        List<Node> targetNodes = findTargetNodes(graphNodes);

        if (targetNodes.size() != 1) {
            errors.add(new FinalNodeError("Exactly one target node is required, but found " + targetNodes.size()));
        } else {
            // If there is exactly one target node, perform further checks on it.
            Node finalTargetNode = targetNodes.get(0);
            if (!(finalTargetNode instanceof LogicNode)) {
                errors.add(new FinalNodeError("The target node must be a LogicNode."));
            } else {
                // Ensure the target node's operator returns a Boolean.
                LogicNode finalLogicNode = (LogicNode) finalTargetNode;
                Operation finalOperation = finalLogicNode.getOperator().getOperationStrategy();
                if (!Boolean.class.isAssignableFrom(finalOperation.getReturnType())) {
                    String errorMessage = "The operator '" + finalLogicNode.getOperator().name() +
                            "' of the target node must return a Boolean, but returns '" +
                            finalOperation.getReturnType().getSimpleName() + "'.";
                    errors.add(new FinalNodeError(errorMessage));
                }
            }
        }

        return errors;
    }

    /**
     * Iterates through all nodes and validates the operator configuration for each LogicNode,
     * collecting any error messages as structured objects.
     */
    private void validateNodeOperators(List<Node> graphNodes, List<ValidationError> errors) {
        for (Node node : graphNodes) {
            if (node instanceof LogicNode) {
                LogicNode logicNode = (LogicNode) node;
                try {
                    Operation strategy = logicNode.getOperator().getOperationStrategy();
                    strategy.validateNode(logicNode);
                } catch (IllegalArgumentException e) {
                    // Create a specific error object for this failure.
                    errors.add(new OperatorConfigurationError(logicNode.getId(), logicNode.getName(), e.getMessage()));
                }
            }
        }
    }

    /**
     * Finds all nodes in the graph that have no outgoing edges (children).
     */
    private List<Node> findTargetNodes(List<Node> graphNodes) {
        Set<Node> parentNodes = new HashSet<>();
        for (Node node : graphNodes) {
            if (node.getInputNodes() != null) {
                for (Node parent : node.getInputNodes()) {
                    if (parent != null) {
                        parentNodes.add(parent);
                    }
                }
            }
        }

        List<Node> targetNodes = new ArrayList<>();
        for (Node node : graphNodes) {
            if (!parentNodes.contains(node)) {
                targetNodes.add(node);
            }
        }
        return targetNodes;
    }
}