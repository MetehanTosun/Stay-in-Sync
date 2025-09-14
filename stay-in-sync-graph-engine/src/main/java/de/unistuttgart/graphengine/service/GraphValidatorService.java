package de.unistuttgart.graphengine.service;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.validation_error.*;
import de.unistuttgart.graphengine.logic_operator.Operation;
import de.unistuttgart.graphengine.nodes.ConfigNode;
import de.unistuttgart.graphengine.nodes.FinalNode;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.graphengine.util.GraphTopologicalSorter;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides a service to validate the structural and logical integrity of a graph.
 */
@ApplicationScoped
public class GraphValidatorService {

    @Inject
    GraphTopologicalSorter sorter;

    /**
     * Performs a comprehensive validation of a graph's structure and nodes.
     */
    public List<ValidationError> validateGraph(List<Node> graphNodes, int originalNodeCount) {
        Log.debugf("Starting validation of graph with %d created nodes (original count: %d)",
                graphNodes != null ? graphNodes.size() : 0, originalNodeCount);
        List<ValidationError> errors = new ArrayList<>();

        if (graphNodes == null || graphNodes.isEmpty()) {
            Log.warn("Graph validation called with null or empty node list.");
            errors.add(new FinalNodeError("Graph cannot be null or empty."));
            return errors;
        }

        validateStructureAndCycles(graphNodes, errors);
        validateFinalNode(graphNodes, errors);
        validateNodeOperators(graphNodes, errors);
        validateConfigNode(graphNodes, errors);

        if (errors.isEmpty()) {
            Log.infof("Graph validation completed successfully for %d nodes.", graphNodes.size());
        } else {
            Log.warnf("Graph validation completed with %d errors.", errors.size());
        }

        return errors;
    }

    /**
     * Validates the basic structure of the graph, such as checking for cycles.
     */
    private void validateStructureAndCycles(List<Node> graphNodes, List<ValidationError> errors) {
        GraphTopologicalSorter.SortResult sortResult = sorter.sort(graphNodes);
        if (sortResult.hasCycle()) {
            Log.warnf("Validation found a cycle involving nodes: %s", sortResult.cycleNodeIds());
            errors.add(new CycleError(sortResult.cycleNodeIds()));
        }
    }

    /**
     * Validates that the graph contains exactly one properly configured FinalNode.
     */
    private void validateFinalNode(List<Node> graphNodes, List<ValidationError> errors) {
        List<FinalNode> finalNodes = findInstances(graphNodes, FinalNode.class);

        if (finalNodes.size() != 1) {
            errors.add(new FinalNodeError("Exactly one FinalNode is required, but found " + finalNodes.size()));
            return;
        }

        FinalNode finalNode = finalNodes.get(0);
        if (graphNodes.size() > 1) {
            if (finalNode.getInputNodes() == null || finalNode.getInputNodes().size() != 1) {
                errors.add(new FinalNodeError("The FinalNode must have exactly one input connection."));
                return;
            }

            Node finalInput = finalNode.getInputNodes().get(0);
            if (finalInput.getOutputType() != Boolean.class) {
                errors.add(new FinalNodeError(String.format(
                        "The FinalNode input must be of type BOOLEAN, but received type '%s' from node '%s'.",
                        finalInput.getOutputType().getSimpleName(), finalInput.getName())));
            }
        }
    }

    /**
     * Iterates through all LogicNodes and validates their internal operator configuration.
     */
    private void validateNodeOperators(List<Node> graphNodes, List<ValidationError> errors) {
        for (Node node : graphNodes) {
            if (node instanceof LogicNode) {
                LogicNode logicNode = (LogicNode) node;
                try {
                    Operation strategy = logicNode.getOperator().getOperationStrategy();
                    strategy.validateNode(logicNode);
                } catch (OperatorValidationException e) {
                    Log.warnf("Validation error for node '%s' (ID: %d): %s", logicNode.getName(), logicNode.getId(), e.getMessage());
                    errors.add(new OperatorConfigurationError(logicNode.getId(), logicNode.getName(), e.getMessage()));
                }
            }
        }
    }

    /**
     * Validates that exactly one ConfigNode exists in the graph.
     */
    private void validateConfigNode(List<Node> graphNodes, List<ValidationError> errors) {
        List<ConfigNode> configNodes = findInstances(graphNodes, ConfigNode.class);

        if (configNodes.size() != 1) {
            errors.add(new ConfigNodeError("Graph Exactly one ConfigNode is required, but found " + configNodes.size()));
        }
    }

    /**
     * A generic helper to find all instances of a specific node type in the graph.
     */
    private <T extends Node> List<T> findInstances(List<Node> nodes, Class<T> type) {
        return nodes.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }
}