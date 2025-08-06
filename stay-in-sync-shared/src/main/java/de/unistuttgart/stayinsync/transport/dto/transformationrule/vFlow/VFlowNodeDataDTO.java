package de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Contains the specific data payload for the logic engine, which is transported
 * within the 'data' field of a VFlowNodeDTO. This includes both the instance-specific
 * data (like its value or path) and the operator's static signature.
 */
@Getter
@Setter
public class VFlowNodeDataDTO {

    /**
     * A human-readable name for the node (e.g., "currentTemperature").
     */
    private String name;

    /**
     * The type discriminator for the logic engine ("PROVIDER", "CONSTANT", "LOGIC", "FINAL").
     */
    private String nodeType;

    /**
     * The system-level identifier for a data source (e.g., an ARC id).
     */
    private Integer arcId;

    /**
     * The full semantic path to the data value for a PROVIDER node.
     */
    private String jsonPath;

    /**
     * The static value for a CONSTANT node.
     */
    private Object value;

    /**
     * The operator type for a LOGIC node (e.g., "ADD", "EQUALS").
     */
    private String operatorType;

    /**
     * A list of strings describing the expected data types for the operator's inputs.
     */
    private List<String> inputTypes;

    /**
     * A string describing the data type of the operator's output.
     */
    private String outputType;

    /**
     * A list defining the limit of edges for each anker point.
     */
    private List<Integer> inputLimit;
}