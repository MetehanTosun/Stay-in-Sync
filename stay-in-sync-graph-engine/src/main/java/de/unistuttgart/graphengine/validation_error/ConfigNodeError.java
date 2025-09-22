package de.unistuttgart.graphengine.validation_error;


/**
 * Represents a validation error related to the graph's ConfigNode configuration.
 * This is used for errors like a missing or duplicated ConfigNode.
 */


public class ConfigNodeError implements ValidationError {
    public ConfigNodeError() {
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private String message;

    public ConfigNodeError(String message) {
        this.message = message;
    }

    @Override
    public String getErrorCode() {
        return "CONFIG_NODE_ERROR";
    }

    @Override
    public String getMessage() {
        return message;
    }
}
