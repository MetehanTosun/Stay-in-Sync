package de.unistuttgart.stayinsync.transport.dto.transformationrule;

import de.unistuttgart.stayinsync.transport.transformation_rule_shared.GraphStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * A Data Transfer Object that represents the complete definition of a logic graph.
 * This is the top-level object for serialization to and from JSON.
 */
@Getter
@Setter
//todo implement annotations (with @Schema?)
public class GraphDTO {
    /**
     * A list containing all the node definitions that make up this graph.
     */
    private List<NodeDTO> nodes;
}