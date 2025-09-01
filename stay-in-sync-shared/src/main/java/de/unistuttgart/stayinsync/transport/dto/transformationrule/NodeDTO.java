package de.unistuttgart.stayinsync.transport.dto.transformationrule;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

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
     * The discriminator field. Expected values: "PROVIDER", "CONSTANT", "LOGIC",
     * "FINAL".
     */
    private String nodeType;

    /**
     * A list of input connections for this node, referencing other nodes by their
     * IDs.
     */
    private List<InputDTO> inputNodes;

    // --- Properties specific to PROVIDER nodes ---
    /**
     * An optional ID for identifying the source system or component (e.g., an ARC
     * id).
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

    // --- Properties specific to CONFIG node ---
    /**
     * Defines the logical condition ('AND' or 'OR') for the change detection.
     * Only used if nodeType is "CONFIG".
     */
    private String changeDetectionMode;

    /**
     * A boolean flag that acts as a bypass switch to enable (true) or disable (false) the change detection logic.
     * Only used if nodeType is "CONFIG".
     */
    private boolean changeDetectionActive;

    /**
     * Enables or disables the time-window validation feature.
     * Only used if nodeType is "CONFIG".
     */
    private boolean timeWindowEnabled;

    /**
     * The duration of the sliding window in milliseconds (e.g., 30000 for 30 seconds).
     * Only used if nodeType is "CONFIG" and timeWindowEnabled is true.
     */
    private long timeWindowMillis;

    // --- Properties specific to LOGIC nodes ---
    /**
     * The name of the logical operator this node performs (e.g., "ADD",
     * "LESS_THAN").
     * Corresponds to a LogicOperator enum constant.
     * Only used if nodeType is "LOGIC".
     */
    private String operatorType;

    /**
     * A list of strings describing the expected data types for the operator's
     * inputs.
     * This is persisted to be available for the UI upon loading the graph.
     */
    private List<String> inputTypes;

    /**
     * A string describing the data type of the operator's output.
     */
    private String outputType;

    /**
     * A list containing two integers [min, max] defining the required number of
     * inputs.
     */
    private List<Integer> inputLimit;

    /**
     * Used to Map the calculatedResult of a Node during evaluation, Important for
     * the Snapshot and reconstruction in the UI
     */
    private Object dynamicValue;
}