package de.unistuttgart.stayinsync.transport.transformation_rule_shared.validation_error;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a validation error related to the graph's terminal node configuration.
 */
@Getter
@NoArgsConstructor(force = true)
public class FinalNodeError implements ValidationError {

    private final String message;

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
