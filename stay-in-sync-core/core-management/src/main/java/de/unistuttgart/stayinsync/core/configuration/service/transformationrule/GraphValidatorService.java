package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;


import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
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
     * <p>
     * This method checks for cycles, operator correctness, and the proper configuration
     * of the mandatory FinalNode.
     *
     * @param graphNodes The list of successfully created nodes from the mapper.
     * @param originalNodeCount The number of nodes in the graph as intended by the user.
     * @return A {@code List<ValidationError>} containing structured error objects. An empty list signifies a valid graph.
     */
    public List<ValidationError> validateGraph(List<Node> graphNodes, int originalNodeCount) {
        Log.debugf("Validating graph with %d created nodes (original count: %d).", graphNodes != null ? graphNodes.size() : 0, originalNodeCount);
        List<ValidationError> errors = new ArrayList<>();
        
        if (originalNodeCount >= 0 && (graphNodes == null || graphNodes.isEmpty())) {
            errors.add(new FinalNodeError("Graph contains no nodes to process."));
            Log.warn("Validation failed: Graph node list is effectively empty, although nodes were expected.");
            return errors;
        }


        // 1. Operator-specific validation on each LogicNode
        validateNodeOperators(graphNodes, errors);

        // 2. Perform topological sort to detect cycles
        GraphTopologicalSorter.SortResult sortResult = sorter.sort(graphNodes);
        if (sortResult.hasCycle()) {
            Log.warnf("Validation found a cycle involving nodes: %s", sortResult.cycleNodeIds());
            errors.add(new CycleError(sortResult.cycleNodeIds()));
        } else {
            Log.debugf("No cycles detected in graph.");
        }

        // 3. Find and validate the FinalNode
        FinalNode finalNode = null;
        int finalNodeCount = 0;
        for (Node node : graphNodes) {
            if (node instanceof FinalNode) {
                finalNode = (FinalNode) node;
                finalNodeCount++;
            }
        }

        // 3a. Check if the COUNT of FinalNodes is correct.
        if (finalNodeCount != 1) {
            errors.add(new FinalNodeError("Exactly one FinalNode is required, but found " + finalNodeCount));
            if (finalNodeCount == 0) {
                Log.warn("Validation failed: No FinalNode found in graph.");
            } else {
                Log.warnf("Validation failed: Found %d FinalNodes, but exactly one is required.", finalNodeCount);
            }
        } else {
            // 3b. IF the count is 1, check the CONNECTION using the original node count.
            if (originalNodeCount > 1 && (finalNode.getInputNodes() == null || finalNode.getInputNodes().isEmpty())) {
                errors.add(new FinalNodeError("The FinalNode has no input from the rest of the graph."));
                Log.warnf("Validation warning: FinalNode '%s' is not connected, but graph was intended to have %d total nodes.",
                        finalNode.getName(), originalNodeCount);
            }
        }

        // Final logging
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
    private void validateNodeOperators(List<Node> graphNodes, List<ValidationError> errors)  {
        Log.debug("Validating individual node operators...");
        for (Node node : graphNodes) {
            if (node instanceof LogicNode) {
                LogicNode logicNode = (LogicNode) node;
                try {
                    Operation strategy = logicNode.getOperator().getOperationStrategy();
                    strategy.validateNode(logicNode);
                } catch (OperatorValidationException  e) {
                    Log.warnf("Validation error for node '%s' (ID: %d): %s", logicNode.getName(), logicNode.getId(), e.getMessage());
                    errors.add(new OperatorConfigurationError(logicNode.getId(), logicNode.getName(), e.getMessage()));
                }
            }
        }
    }
}
