package de.unistuttgart.stayinsync.syncnode.logik_engine;

import java.util.Map;

/**
 * Represents an input provider that sources its value from a user interface (UI) element.
 * The specific UI element is identified by its name.
 */
public class ConstantNode implements InputNode {
    private final  String elementName;
    private final Object value;


    public ConstantNode(String name, Object value) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name for ConstantNode cannot be null or empty.");
        }
        this.elementName = name;
        this.value = value;
    }

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
     * @return false, as this provider is not sourced from an external JSON path.
     */
    @Override
    public boolean isJsonNode() {
        return false;
    }

    /**
     * @return true, as this provider is sourced from a UI element.
     */
    @Override
    public boolean isConstantNode() {
        return true;
    }

    /**
     * This operation is not supported for UIInput.
     * @throws UnsupportedOperationException always.
     */
    @Override
    public LogicNode getParentNode() {
        throw new UnsupportedOperationException("UIInput does not provide a parent node.");
    }

}