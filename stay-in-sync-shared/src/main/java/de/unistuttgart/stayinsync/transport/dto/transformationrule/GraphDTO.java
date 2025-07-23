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
     * The unique database ID of the graph entity.
     */
    private Long id;

    /**
     * The unique, human-readable name for this graph.
     */
    private String name;

    /**
     * A list containing all the node definitions that make up this graph.
     */
    private List<NodeDTO> nodes;

    /**
     * Represents the validation status of the graph.
     * This field is determined by the backend during a create or update operation.
     * The frontend can use this status to inform the user whether the graph
     * is a finalized, executable rule or an invalid draft.
     */
    private GraphStatus status;
}