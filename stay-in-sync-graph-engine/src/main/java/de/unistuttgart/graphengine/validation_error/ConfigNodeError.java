package de.unistuttgart.graphengine.validation_error;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a validation error related to the graph's ConfigNode configuration.
 * This is used for errors like a missing or duplicated ConfigNode.
 */
@Getter
@Setter
@NoArgsConstructor(force = true)
public class ConfigNodeError implements ValidationError {

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
