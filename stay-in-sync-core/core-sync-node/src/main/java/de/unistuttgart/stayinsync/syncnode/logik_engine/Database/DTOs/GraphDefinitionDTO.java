package de.unistuttgart.stayinsync.syncnode.logik_engine.Database.DTOs;

import java.util.List;

/**
 * Data Transfer Object (DTO) that represents the complete definition of a logic graph.
 * This object is typically serialized to and deserialized from a JSON string in the database.
 */
public class GraphDefinitionDTO {

    /**
     * A list containing all the node definitions that make up this graph.
     */
    public List<NodeDTO> nodes;
}
