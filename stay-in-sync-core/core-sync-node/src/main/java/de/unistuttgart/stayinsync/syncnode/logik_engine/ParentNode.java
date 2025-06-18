package de.unistuttgart.stayinsync.syncnode.logik_engine;

import jakarta.json.JsonObject;

import java.util.Map;

/**
 * Represents an input provider that sources its value from the calculated result
 * of another {@link LogicNode} (referred to as the parent node).
 */
public class ParentNode implements InputNode {
    private final LogicNode parentNode; // The LogicNode from which this input is sourced.

    /**
     * Constructs a ParentNode.
     *
     * @param parentNode The {@link LogicNode} whose result will be used as the input.
     *                   Must not be null.
     * @throws IllegalArgumentException if parentNode is null.
     */
    public ParentNode(LogicNode parentNode) {
        if (parentNode == null) {
            throw new IllegalArgumentException("ParentNode in NodeInput cannot be null.");
        }
        this.parentNode = parentNode;
    }

    /**
     * Retrieves the calculated result from the linked parent LogicNode.
     *
     * @return The calculated result of the parent LogicNode.
     * @throws IllegalStateException if the parent node's result is null (indicating it wasn't evaluated).
     */
    @Override
    public Object getValue(Map<String, JsonObject> context) {

        Object result = this.parentNode.getCalculatedResult();
        if (result == null) {

            throw new IllegalStateException("Result for parent node '" + this.parentNode.getNodeName() + "' is null. " +
                    "This indicates it was not evaluated before being accessed by a child node.");
        }
        return result;
    }

    /**
     * @return true, as this provider is sourced from another LogicNode.
     */
    @Override
    public boolean isParentNode() {
        return true;
    }

    /**
     * @return false, as this provider is not sourced from a JsonNode.
     */
    @Override
    public boolean isJsonNode() {
        return false;
    }

    /**
     * @return false, as this provider is not sourced from a ConstantNode.
     */
    @Override
    public boolean isConstantNode() {
        return false;
    }

    /**
     * @return The parent {@link LogicNode} from which this input is sourced.
     */
    @Override
    public LogicNode getParentNode() {
        return this.parentNode;
    }

}