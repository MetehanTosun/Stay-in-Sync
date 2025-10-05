package de.unistuttgart.graphengine.validation_error;

import com.fasterxml.jackson.annotation.JsonTypeName;


/**
 * Represents a validation error indicating that a node could not be
 * constructed from its DTO due to invalid or missing configuration.
 * This is a fundamental structural error in the graph definition.
 */
@JsonTypeName("NODE_CONFIG_ERROR")
public class NodeConfigurationError implements ValidationError {

    private  int nodeId;
    private  String nodeName;
    private  String message;

    public NodeConfigurationError() {
    }

    public NodeConfigurationError(int nodeId, String nodeName, String message) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.message = message;
    }

    public int getNodeId() {
        return this.nodeId;
    }

    @Override
    public String getErrorCode() {
        return "NODE_CONFIG_ERROR";
    }

    @Override
    public String getMessage() {
        String namePart = (nodeName != null && !nodeName.trim().isEmpty()) ? " ('" + nodeName + "')" : "";
        return "Node_ID " + nodeId + namePart + " is invalid: " + message;
    }
}
