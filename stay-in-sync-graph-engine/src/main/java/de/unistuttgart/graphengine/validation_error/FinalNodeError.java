package de.unistuttgart.graphengine.validation_error;

/**
 * Represents a validation error related to the graph's terminal node configuration.
 */

public class FinalNodeError implements ValidationError {

    public void setMessage(String message) {
        this.message = message;
    }

    public FinalNodeError() {
    }

    private  String message;

    public FinalNodeError(String message) {
        this.message = message;
    }

    @Override
    public String getErrorCode() {
        return "FINAL_NODE_ERROR";
    }

    @Override
    public String getMessage() {
        return message;
    }
}
