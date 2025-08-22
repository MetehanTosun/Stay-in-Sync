package de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.LogicOperator;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.Operation;
import lombok.Getter;
import lombok.Setter;
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
     * @param operator The logical operation this node will perform.
     */
    public LogicNode(String name, LogicOperator operator) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name for LogicNode cannot be null or empty.");
        }
        if (operator == null) {
            throw new IllegalArgumentException("Operator for LogicNode cannot be null.");
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
    public LogicNode(String name, LogicOperator operator, Node... inputs) {
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
     * @param dataContext The runtime data context, which is passed down to the strategy.
     */
    @Override
    public void calculate(Map<String, JsonNode> dataContext) {
        // 1. Retrieve the specific strategy implementation from the operator.
        Operation strategy = this.getOperator().getOperationStrategy();

        // 2. Delegate the actual execution to the strategy.
        Object result = strategy.execute(this, dataContext);

        // 3. Store the computed result internally.
        this.setCalculatedResult(result);
    }

    public LogicOperator getOperator() {
        return operator;
    }

    public void setOperator(LogicOperator operator) {
        this.operator = operator;
    }
}