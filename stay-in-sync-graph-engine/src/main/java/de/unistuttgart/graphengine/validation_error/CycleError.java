package de.unistuttgart.graphengine.validation_error;

import java.util.List;

/**
 * Represents a validation error indicating that a cycle was detected in the graph.
 */

public class CycleError implements ValidationError {
    public List<Integer> getNodeIdsInCycle() {
        return nodeIdsInCycle;
    }

    public void setNodeIdsInCycle(List<Integer> nodeIdsInCycle) {
        this.nodeIdsInCycle = nodeIdsInCycle;
    }

    public CycleError() {
    }

    private  List<Integer> nodeIdsInCycle;

    public CycleError(List<Integer> nodeIdsInCycle) {
        this.nodeIdsInCycle = nodeIdsInCycle;
    }

    @Override
    public String getErrorCode() {
        return "CYCLE_DETECTED";
    }

    @Override
    public String getMessage() {
        return "A cycle was detected involving nodes: " + nodeIdsInCycle;
    }
}
