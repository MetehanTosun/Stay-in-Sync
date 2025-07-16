package de.unistuttgart.stayinsync.transport.transformation_rule_shared.validation_error;

import lombok.Getter;

/**
 * Represents a validation error related to the graph's terminal node configuration.
 */
@Getter
public class FinalNodeError implements ValidationError {

    private final String message;

    public FinalNodeError(String message) {
        this.message = message;
    }

    @Override
    public String getErrorCode() {
        return "INVALID_FINAL_NODE";
    }

    @Override
    public String getMessage() {
        return message;
    }
}
