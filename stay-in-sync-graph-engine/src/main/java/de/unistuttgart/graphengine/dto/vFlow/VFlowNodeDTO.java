package de.unistuttgart.graphengine.dto.vFlow;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a single node from ngx-vflow.
 */
@Getter
@Setter
public class VFlowNodeDTO {
    /**
     * The unique identifier of the node, typically a string.
     */
    private String id;

    /**
     * The visual position of the node on the canvas.
     */
    private PointDTO point;

    /**
     * The type of the node, used by the frontend to render the correct component.
     */
    private String type;

    /**
     * The visual width of the node.
     */
    private double width;

    /**
     * The visual height of the node.
     */
    private double height;

    /**
     * The data payload containing specific information for the logic engine.
     */
    private VFlowNodeDataDTO data;
}
