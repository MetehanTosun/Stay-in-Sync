package de.unistuttgart.graphengine.nodes;

import de.unistuttgart.graphengine.exception.NodeConfigurationException;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * A node that holds a constant, predefined value.
 */
@Getter
@Setter
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
     * @throws NodeConfigurationException if name or value are null or empty.
     */
    public ConstantNode(String name, Object value) throws NodeConfigurationException {
        if (name == null || name.trim().isEmpty()) {
            throw new NodeConfigurationException("Name for ConstantNode cannot be null or empty.");
        }
        if (value == null) {
            throw new NodeConfigurationException("Value for ConstantNode cannot be null.");
        }
        this.setName(name);
        this.value = value;
    }

    @Override
    public void calculate(Map<String, Object> dataContext) {
        this.setCalculatedResult(this.getValue());
    }

    @Override
    public Class<?> getOutputType() {
        if (value == null) {
            return Object.class;
        }
        return value.getClass();
    }
}