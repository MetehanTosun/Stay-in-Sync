package de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a single edge (connection) from ngx-vflow, including its source,
 * target, and visual styling information.
 */
@Getter
@Setter
public class VFlowEdgeDTO {
    /**
     * The unique identifier of the edge (e.g., "0 -> 2").
     */
    private String id;

    /**
     * The ID of the source (parent) node.
     */
    private String source;

    /**
     * The ID of the target (child) node.
     */
    private String target;

    /**
     * The identifier of the specific input handle on the target node
     * (e.g., "input-0", "input-1"). This is used by the backend to determine the orderIndex.
     */
    private String targetHandle;

}
