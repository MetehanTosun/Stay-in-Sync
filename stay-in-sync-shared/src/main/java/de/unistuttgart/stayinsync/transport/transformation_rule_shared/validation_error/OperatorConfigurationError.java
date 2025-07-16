package de.unistuttgart.stayinsync.transport.transformation_rule_shared.validation_error;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a validation error indicating that a specific node is incorrectly
 * configured for its assigned operator.
 */
@Getter
public class OperatorConfigurationError implements ValidationError {

    private final int nodeId;
    private final String nodeName;
    private final String message;

    public OperatorConfigurationError(int nodeId, String nodeName, String message) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.message = message;
    }

    @Override
    public String getErrorCode() {
        return "OPERATOR_CONFIG_ERROR";
    }

    @Override
    public String getMessage() {
        return "Node '" + nodeName + "' (ID: " + nodeId + "): " + message;
    }
}
