package de.unistuttgart.stayinsync.transport.dto.transformationrule;

import lombok.Getter;
import lombok.Setter;

/**
 * A Data Transfer Object representing an edge in the graph.
 * It defines an input for a node by referencing the ID of the parent node.
 */
@Getter
@Setter
public class InputDTO {

    /**
     * The unique ID of the node that provides the input.
     */
    private int id;

    /**
     * The zero-based index specifying the order of this input for the receiving node.
     */
    private int orderIndex;
}