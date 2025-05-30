package de.unistuttgart.stayinsync.syncnode.logik_engine;

/**
 * Represents an input provider that sources its value from a specific path
 * within an external JSON object (typically representing AAS data).
 */
public class ExternalInput implements InputProvider {
    private final String jsonPath; // The path to the value within the JSON object.

    /**
     * Constructs an ExternalInput.
     *
     * @param jsonPath The path to the value in the JSON object. Must not be null or empty.
     * @throws IllegalArgumentException if jsonPath is null or empty.
     */
    public ExternalInput(String jsonPath) {
        if (jsonPath == null || jsonPath.trim().isEmpty()) {
            throw new IllegalArgumentException("JSON path in ExternalInput cannot be null or empty.");
        }
        this.jsonPath = jsonPath;
    }

    /**
     * @return false, as this provider is not sourced from another LogicNode.
     */
    @Override
    public boolean isNodeSource() {
        return false;
    }

    /**
     * @return true, as this provider is sourced from an external JSON path.
     */
    @Override
    public boolean isExternalSource() {
        return true;
    }

    /**
     * @return false, as this provider is not sourced from a UI element.
     */
    @Override
    public boolean isUISource() {
        return false;
    }

    /**
     * This operation is not supported for ExternalInput.
     * @throws UnsupportedOperationException always.
     */
    @Override
    public LogicNode getParentNode() {
        throw new UnsupportedOperationException("ExternalInput does not provide a parent node.");
    }

    /**
     * This operation is not supported for ExternalInput.
     * @throws UnsupportedOperationException always.
     */
    @Override
    public String getUiElementName() {
        throw new UnsupportedOperationException("ExternalInput does not provide a UI element name.");
    }

    /**
     * @return The JSON path string that identifies the location of the input value.
     */
    @Override
    public String getExternalJsonPath() {
        return this.jsonPath;
    }
}