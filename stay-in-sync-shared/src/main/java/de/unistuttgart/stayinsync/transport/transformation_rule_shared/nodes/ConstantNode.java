package de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes;


import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class ConstantNode extends Node {

    /**
     * The static value of the constant.
     */
    private Object value;

    /**
     * Constructs a new ConstantNode with a specific name and value.
     *
     * @param name  A human-readable name for this constant (e.g., "ThresholdValue"). Cannot be null or empty.
     * @param value The constant value for this node. Cannot be null.
     * @throws IllegalArgumentException if name or value are null or empty.
     */
    public ConstantNode(String name, Object value) {
        // Validation: A ConstantNode must have a valid name and value.
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name for ConstantNode cannot be null or empty.");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value for ConstantNode cannot be null.");
        }
        this.setName(name);
        this.value = value;
    }

    @Override
    public void calculate(Map<String, JsonNode> dataContext) {
        this.setCalculatedResult(this.getValue());
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}