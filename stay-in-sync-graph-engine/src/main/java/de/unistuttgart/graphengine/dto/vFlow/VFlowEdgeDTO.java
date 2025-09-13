package de.unistuttgart.graphengine.dto.vFlow;


/**
 * Represents a single edge (connection) from ngx-vflow, including its source,
 * target, and visual styling information.
 */

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTargetHandle() {
        return targetHandle;
    }

    public void setTargetHandle(String targetHandle) {
        this.targetHandle = targetHandle;
    }
}
