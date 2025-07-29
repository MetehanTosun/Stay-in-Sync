package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;


import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.FinalNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.util.GraphTopologicalSorter;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.validation_error.*;
import io.quarkus.logging.Log;
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
     * This method checks for cycles, operator correctness, and the proper configuration
     * of the mandatory FinalNode.
     *
     * @param graphNodes The complete list of nodes in the graph.
     * @return A {@code List<ValidationError>} containing structured error objects. An empty list signifies a valid graph.
     */
    public List<ValidationError> validateGraph(List<Node> graphNodes) {
        Log.debugf("Validating graph with %d nodes.", graphNodes != null ? graphNodes.size() : 0);
        List<ValidationError> errors = new ArrayList<>();

        if (graphNodes == null || graphNodes.isEmpty()) {
            errors.add(new FinalNodeError("Graph node list cannot be null or empty."));
            Log.warn("Validation failed: Graph node list is null or empty.");
            return errors;
        }

        // 1. Operator-specific validation on each LogicNode (remains the same).
        validateNodeOperators(graphNodes, errors);

        // 2. Perform topological sort to detect cycles (remains the same).
        GraphTopologicalSorter.SortResult sortResult = sorter.sort(graphNodes);
        if (sortResult.hasCycle()) {
            Log.warnf("Validation found a cycle involving nodes: %s", sortResult.cycleNodeIds());
            errors.add(new CycleError(sortResult.cycleNodeIds()));
        }
        else {
            Log.debugf("No cycles detected in graph.");
        }

        FinalNode finalNode = null;
        int finalNodeCount = 0;
        for (Node node : graphNodes) {
            if (node instanceof FinalNode) {
                finalNode = (FinalNode) node;
                finalNodeCount++;
            }
        }

        // Check if exactly one FinalNode exists.
        if (finalNodeCount != 1) {
            errors.add(new FinalNodeError("Exactly one FinalNode is required in the graph, but found " + finalNodeCount));
            if(finalNodeCount == 0){
                Log.warnf("Validation failed: No FinalNode found in graph. Every graph must have exactly one FinalNode.");
            }
            else {
                Log.warnf("Validation failed: Found %d FinalNodes in graph, but exactly one is required.", finalNodeCount);
            }
        } else {
            // ensure the FinalNode is connected.
            if (graphNodes.size() > 1 && (finalNode.getInputNodes() == null || finalNode.getInputNodes().isEmpty())) {
                errors.add(new FinalNodeError("The FinalNode has no input from the rest of the graph."));
                Log.warnf("Validation warning: FinalNode '%s' is not connected to any input nodes, but graph contains %d total nodes.",
                        finalNode.getName(), graphNodes.size());
            }
        }

        if (errors.isEmpty()) {
            Log.infof("Graph validation completed successfully for %d nodes. No errors found.", graphNodes.size());
        } else {
            Log.warnf("Graph validation completed with %d errors found: %s", errors.size(),
                    errors.stream().map(ValidationError::getMessage).collect(Collectors.toList()));
        }

        Log.debugf("Returning %d validation errors.", errors.size());
        return errors;
    }

    /**
     * Iterates through all nodes and validates the operator configuration for each LogicNode.
     */
    private void validateNodeOperators(List<Node> graphNodes, List<ValidationError> errors) {
        Log.debug("Validating individual node operators...");
        for (Node node : graphNodes) {
            if (node instanceof LogicNode) {
                LogicNode logicNode = (LogicNode) node;
                try {
                    // This logic remains the same
                    Operation strategy = logicNode.getOperator().getOperationStrategy();
                    strategy.validateNode(logicNode);
                } catch (IllegalArgumentException e) {
                    Log.warnf("Validation error for node '%s' (ID: %d): %s", logicNode.getName(), logicNode.getId(), e.getMessage());
                    errors.add(new OperatorConfigurationError(logicNode.getId(), logicNode.getName(), e.getMessage()));
                }
            }
        }
    }
}