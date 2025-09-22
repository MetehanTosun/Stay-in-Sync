package de.unistuttgart.graphengine.nodes;

import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.exception.NodeConfigurationException;
import de.unistuttgart.graphengine.logic_operator.LogicOperator;
import de.unistuttgart.graphengine.logic_operator.Operation;

import java.util.Arrays;
import java.util.Map;

/**
 * A node that performs a logical or arithmetic operation on its inputs.
 */

public class LogicNode extends Node {

    private LogicOperator operator;

    /**
     * Constructor for mappers, creating a node with its essential properties.
     * Inputs are set later in a second pass.
     *
     * @param name     The unique, human-readable name for this node.
     * @throws NodeConfigurationException if the name or operator are null/empty.
     */
    public LogicNode(String name, LogicOperator operator) throws NodeConfigurationException {
        if (name == null || name.trim().isEmpty()) {
            throw new NodeConfigurationException("Name for LogicNode cannot be null or empty.");
        }
        if (operator == null) {
            throw new NodeConfigurationException("Operator for LogicNode cannot be null.");
        }
        this.setName(name);
        this.operator = operator;
    }

    /**
     * Constructs a new LogicNode
     *
     * @param name     The unique, human-readable name for this node.
     * @param operator The logical operation this node will perform.
     * @param inputs   The parent nodes that provide input for this node.
     */
    public LogicNode(String name, LogicOperator operator, Node... inputs) throws NodeConfigurationException {
        this(name, operator); // Calls the main constructor to set and validate name and operator
        this.setInputNodes(Arrays.asList(inputs));
    }

    /**
     * Executes the calculation for this LogicNode.
     * <p>
     * This method implements the core of the <b>Strategy Pattern</b>. It does not perform any
     * logical computation itself. Instead, it retrieves the appropriate {@link Operation}
     * strategy based on its configured {@link LogicOperator} and delegates the
     * execution to it.
     * <p>
     * The strategy's {@code execute} method is then responsible for accessing the
     * pre-calculated results from this node's inputs (via {@code this.getInputNodes()})
     * and performing the actual operation.
     *
     * @throws GraphEvaluationException if the underlying operation strategy fails during execution
     * (e.g., due to a type mismatch).
     */
    @Override
    public void calculate(Map<String, Object> dataContext) throws GraphEvaluationException {
        Operation strategy = this.getOperator().getOperationStrategy();

        try {
            Object result = strategy.execute(this, dataContext);
            this.setCalculatedResult(result);
        } catch (GraphEvaluationException e) {
            throw e;
        } catch (Exception e) {
            throw new GraphEvaluationException(
                    GraphEvaluationException.ErrorType.EXECUTION_FAILED,
                    "Execution Failed",
                    "An unexpected error occurred in operator '" + this.getOperator() + "' for node '" + this.getName() + "'.",
                    e
            );
        }
    }

    @Override
    public Class<?> getOutputType() {
        if (operator == null) {
            return Object.class;
        }
        return getOperator().getOperationStrategy().getReturnType();
    }

    public LogicOperator getOperator() {
        return operator;
    }

    public void setOperator(LogicOperator operator) {
        this.operator = operator;
    }
}