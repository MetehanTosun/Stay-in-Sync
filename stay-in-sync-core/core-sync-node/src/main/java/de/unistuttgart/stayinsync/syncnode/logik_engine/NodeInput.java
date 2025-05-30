package de.unistuttgart.stayinsync.syncnode.logik_engine;

/**
 * Represents an input provider that sources its value from the calculated result
 * of another {@link LogicNode} (referred to as the parent node).
 */
public class NodeInput implements InputProvider {
    private final LogicNode parentNode; // The LogicNode from which this input is sourced.

    /**
     * Constructs a NodeInput.
     *
     * @param parentNode The {@link LogicNode} whose result will be used as the input.
     *                   Must not be null.
     * @throws IllegalArgumentException if parentNode is null.
     */
    public NodeInput(LogicNode parentNode) {
        if (parentNode == null) {
            throw new IllegalArgumentException("ParentNode in NodeInput cannot be null.");
        }
        this.parentNode = parentNode;
    }

    /**
     * @return true, as this provider is sourced from another LogicNode.
     */
    @Override
    public boolean isNodeSource() {
        return true;
    }

    /**
     * @return false, as this provider is not sourced from an external JSON path.
     */
    @Override
    public boolean isExternalSource() {
        return false;
    }

    /**
     * @return false, as this provider is not sourced from a UI element.
     */
    @Override
    public boolean isUISource() {
        return false;
    }

    /**
     * @return The parent {@link LogicNode} from which this input is sourced.
     */
    @Override
    public LogicNode getParentNode() {
        return this.parentNode;
    }

    /**
     * This operation is not supported for NodeInput.
     * @throws UnsupportedOperationException always.
     */
    @Override
    public String getExternalJsonPath() {
        throw new UnsupportedOperationException("NodeInput does not provide an external JSON path.");
    }

    /**
     * This operation is not supported for NodeInput.
     * @throws UnsupportedOperationException always.
     */
    @Override
    public String getUiElementName() {
        throw new UnsupportedOperationException("NodeInput does not provide a UI element name.");
    }
}