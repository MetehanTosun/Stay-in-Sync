package de.unistuttgart.graphengine.validation_error;



/**
 * Represents a validation error indicating that a specific node is incorrectly
 * configured for its assigned operator.
 */

public class OperatorConfigurationError implements ValidationError {

    private  int nodeId;
    private  String nodeName;
    private  String message;

    public OperatorConfigurationError(int nodeId, String nodeName, String message) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.message = message;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OperatorConfigurationError() {
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
