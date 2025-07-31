package de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.GraphEvaluationException;
import de.unistuttgart.stayinsync.transport.exception.OperatorValidationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;

import java.util.Map;

/**
 * Strategy-Interface für eine Rechenoperation innerhalb der Logik-Engine.
 * Jede Implementierung kapselt die Logik für einen bestimmten LogicOperator.
 */
public interface Operation {

    /**
     * Calculates the result of the operation based on the node's state.
     *
     * @param node        The LogicNode currently being evaluated.
     * @param dataContext The runtime data context.
     * @return The result of the calculation.
     */
    Object execute(LogicNode node, Map<String, JsonNode> dataContext) throws GraphEvaluationException;

    /**
     * Validates the node to ensure it is correctly configured for this operation
     * (e.g., correct number and type of inputs).
     * @param node The LogicNode to validate.
     * @throws IllegalArgumentException if the configuration is invalid.
     */
    void validateNode(LogicNode node) throws OperatorValidationException;;

    /**
     * Returns the Java type of the value that this operation produces.
     * @return The Class of the return type (e.g., Boolean.class, Double.class).
     */
    Class<?> getReturnType();
}