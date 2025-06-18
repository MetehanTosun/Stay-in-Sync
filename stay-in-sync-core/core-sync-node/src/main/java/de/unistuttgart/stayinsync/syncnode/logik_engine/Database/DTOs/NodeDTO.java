package de.unistuttgart.stayinsync.syncnode.logik_engine.Database.DTOs;

import java.util.List;

/**
 * Data Transfer Object (DTO) for a single {@link de.unistuttgart.stayinsync.syncnode.logik_engine.LogicNode}.
 * It holds the serializable properties of a node.
 */
public class NodeDTO {

    /**
     * The unique name of the node within the graph (e.g., "CorrectedTemperature").
     */
    public String nodeName;

    /**
     * The string representation of the operator (e.g., "ADD", "GREATER_THAN").
     * This corresponds to the name of the {@link de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator} enum constant.
     */
    public String operator;

    /**
     * A list of input definitions for this node.
     */
    public List<InputDTO> inputs;
}
