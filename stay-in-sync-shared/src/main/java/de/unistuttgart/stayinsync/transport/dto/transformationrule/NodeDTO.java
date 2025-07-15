package de.unistuttgart.stayinsync.transport.dto.transformationrule;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * A "flat" Data Transfer Object for a single node in the logic graph.
 * It contains all possible properties for any node type. The 'nodeType' field
 * acts as a discriminator to determine which properties are relevant.
 */
@Getter
@Setter
public class NodeDTO {

    /**
     * A unique identifier for the node within the graph.
     */
    private int id;

    /**
     * An optional, human-readable name for the node (e.g., "CorrectedTemperature").
     */
    private String name;

    /**
     * The visual x-coordinate for the node's position in a UI.
     * This is ignored by the evaluation engine.
     */
    private double offsetX;

    /**
     * The visual y-coordinate for the node's position in a UI.
     * This is ignored by the evaluation engine.
     */
    private double offsetY;

    /**
     * The discriminator field. Expected values: "PROVIDER", "CONSTANT", "LOGIC".
     */
    private String nodeType;

    /**
     * A list of input connections for this node, referencing other nodes by their IDs.
     */
    private List<InputDTO> inputNodes;

    // --- Properties specific to PROVIDER nodes ---
    /**
     * An optional ID for identifying the source system or component (e.g., an ARC id).
     * Only used if nodeType is "PROVIDER".
     */
    private Integer arcId;
    /**
     * The JSON-Path used to extract a value from the provider's data source.
     * Only used if nodeType is "PROVIDER".
     */
    private String jsonPath;

    // --- Properties specific to CONSTANT nodes ---
    /**
     * The static value of the constant. Can be a Boolean, String, Number, etc.
     * Only used if nodeType is "CONSTANT".
     */
    private Object value;

    // --- Properties specific to LOGIC nodes ---
    /**
     * The name of the logical operator this node performs (e.g., "ADD", "LESS_THAN").
     * Corresponds to a LogicOperator enum constant.
     * Only used if nodeType is "LOGIC".
     */
    private String operatorType;
}