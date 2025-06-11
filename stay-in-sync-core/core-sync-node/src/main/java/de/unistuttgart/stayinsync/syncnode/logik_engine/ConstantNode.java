package de.unistuttgart.stayinsync.syncnode.logik_engine;

import lombok.Getter;

/**
 * Represents an input node that provides a constant value.
 * This value is defined at the time the ConstantNode is created.
 * The provided value for a constant cannot be null.
 */
public class ConstantNode implements InputNode {
    /**
     * -- GETTER --
     *  Returns the descriptive name assigned to this constant node.
     *  This name was provided during construction and is useful for debugging or identification.
     *
     * @return The name of this constant.
     */
    @Getter
    private final  String elementName;
    private final Object value;

    /**
     * Constructs a new ConstantNode.
     *
     * @param name  A descriptive name for this constant (e.g., "ThresholdValue", "IsEnabledFlag").
     *              This name is primarily for identification or debugging purposes.
     *              Must not be null or empty.
     * @param value The constant value this node will provide. This value cannot be null.
     * @throws IllegalArgumentException if the provided name is null or empty, or if the value is null.
     */
    public ConstantNode(String name, Object value) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name for ConstantNode cannot be null or empty.");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value for ConstantNode (name: '" + name + "') cannot be null.");
        }

        this.elementName = name;
        this.value = value;
    }

    /**
     * Returns the constant value held by this node.
     * This value is guaranteed to be non-null due to constructor validation.
     *
     * @return The pre-defined, non-null constant object.
     */
    @Override
    public Object getValue() {
        return value;
    }

    /**
     * @return false, as this provider is not sourced from another LogicNode.
     */
    @Override
    public boolean isParentNode() {
        return false;
    }

    /**
     * @return false, as this provider is not sourced from a JsonNode.
     */
    @Override
    public boolean isJsonNode() {
        return false;
    }

    /**
     * @return true, as this provider is sourced from a ConstantNode.
     */
    @Override
    public boolean isConstantNode() {
        return true;
    }

    /**
     * @throws UnsupportedOperationException always, as ConstantNode does not have a parent LogicNode.
     */
    @Override
    public LogicNode getParentNode() {
        throw new UnsupportedOperationException("UIInput does not provide a parent node.");
    }

}