package de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow;

import lombok.Getter;
import lombok.Setter;


/**
 * A DTO representing the visual styling and metadata for a VFlowEdge.
 */
@Getter
@Setter
public class VFlowEdgeStyleDTO {
    /**
     * The width of the edge's line in pixels.
     */
    private Integer strokeWidth;

    /**
     * The color of the edge's line (e.g., a hex code like "#ec586e").
     */
    private String color;
}
