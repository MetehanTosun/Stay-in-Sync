package de.unistuttgart.stayinsync.transport.dto.transformationrule;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * A Data Transfer Object that represents the complete definition of a logic graph.
 * This is the top-level object for serialization to and from JSON.
 */
@Getter
@Setter
public class GraphDTO {

    /**
     * The unique, human-readable name for this graph.
     */
    private String name;

    /**
     * A list containing all the node definitions that make up this graph.
     */
    private List<NodeDTO> nodes;
}