package de.unistuttgart.stayinsync.syncnode.logik_engine.database.DTOs;

import de.unistuttgart.stayinsync.syncnode.logik_engine.logic_operator.LogicOperator;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.LogicNode;

import java.util.List;

/**
 * Data Transfer Object (DTO) for a single {@link LogicNode}.
 * It holds the serializable properties of a node.
 */
public class NodeDTO {

    /**
     * The unique name of the node within the graph (e.g., "CorrectedTemperature").
     */
    public String nodeName;

    /**
     * The string representation of the operator (e.g., "ADD", "GREATER_THAN").
     * This corresponds to the name of the {@link LogicOperator} enum constant.
     */
    public String operator;

    /**
     * A list of input definitions for this node.
     */
    public List<InputDTO> inputs;
}
