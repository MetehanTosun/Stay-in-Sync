package de.unistuttgart.stayinsync.syncnode.logik_engine;

/**
 * Represents an input provider that sources its value from a user interface (UI) element.
 * The specific UI element is identified by its name.
 */
public class UIInput implements InputProvider {
    private final String uiElementName; // The unique name or identifier for the UI element.

    /**
     * Constructs a UIInput.
     *
     * @param uiElementName The name or identifier of the UI element from which
     *                      the value will be sourced (e.g., "thermostatSlider", "enableFeatureCheckbox").
     *                      Must not be null or empty.
     * @throws IllegalArgumentException if uiElementName is null or empty.
     */
    public UIInput(String uiElementName) {
        if (uiElementName == null || uiElementName.trim().isEmpty()) {
            throw new IllegalArgumentException("UI element name in UIInput cannot be null or empty.");
        }
        this.uiElementName = uiElementName;
    }

    /**
     * @return false, as this provider is not sourced from another LogicNode.
     */
    @Override
    public boolean isNodeSource() {
        return false;
    }

    /**
     * @return false, as this provider is not sourced from an external JSON path.
     */
    @Override
    public boolean isExternalSource() {
        return false;
    }

    /**
     * @return true, as this provider is sourced from a UI element.
     */
    @Override
    public boolean isUISource() {
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

    /**
     * This operation is not supported for UIInput.
     * @throws UnsupportedOperationException always.
     */
    @Override
    public String getExternalJsonPath() {
        throw new UnsupportedOperationException("UIInput does not provide an external JSON path.");
    }

    /**
     * @return The name or identifier of the UI element from which this input is sourced.
     */
    @Override
    public String getUiElementName() {
        return this.uiElementName;
    }
}