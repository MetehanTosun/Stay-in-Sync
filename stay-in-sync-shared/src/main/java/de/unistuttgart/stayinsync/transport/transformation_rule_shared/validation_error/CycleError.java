package de.unistuttgart.stayinsync.transport.transformation_rule_shared.validation_error;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Represents a validation error indicating that a cycle was detected in the graph.
 */
@Getter
public class CycleError implements ValidationError {

    private final List<Integer> nodeIdsInCycle;

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
