package de.unistuttgart.stayinsync.transport.transformation_rule_shared.validation_error;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a validation error indicating that a specific node is incorrectly
 * configured for its assigned operator.
 */
@Getter
@Setter
@NoArgsConstructor(force = true)
public class OperatorConfigurationError implements ValidationError {

    private  int nodeId;
    private  String nodeName;
    private  String message;

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
        return "Node_ID: " + nodeId + " : "  + message;
    }
}
