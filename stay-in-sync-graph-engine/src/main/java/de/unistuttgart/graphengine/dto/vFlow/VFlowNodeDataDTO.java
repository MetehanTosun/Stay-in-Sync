package de.unistuttgart.graphengine.dto.vFlow;



import java.util.List;

/**
 * Contains the specific data payload for the logic engine, which is transported
 * within the 'data' field of a VFlowNodeDTO. This includes both the instance-specific
 * data (like its value or path) and the operator's static signature.
 */

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

    /**
     * The currently set mode of a config node
     */
    private String changeDetectionMode;

    /**
     * A Boolean describing if the config node's mode is active or not
     */
    private boolean changeDetectionActive;

    /**
     * A Long representing the time window a config node checks for changes
     */
    private Long timeWindowMillis;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public Integer getArcId() {
        return arcId;
    }

    public void setArcId(Integer arcId) {
        this.arcId = arcId;
    }

    public String getJsonPath() {
        return jsonPath;
    }

    public void setJsonPath(String jsonPath) {
        this.jsonPath = jsonPath;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getOperatorType() {
        return operatorType;
    }

    public void setOperatorType(String operatorType) {
        this.operatorType = operatorType;
    }

    public List<String> getInputTypes() {
        return inputTypes;
    }

    public void setInputTypes(List<String> inputTypes) {
        this.inputTypes = inputTypes;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public List<Integer> getInputLimit() {
        return inputLimit;
    }

    public void setInputLimit(List<Integer> inputLimit) {
        this.inputLimit = inputLimit;
    }

    public String getChangeDetectionMode() {
        return changeDetectionMode;
    }

    public void setChangeDetectionMode(String changeDetectionMode) {
        this.changeDetectionMode = changeDetectionMode;
    }

    public boolean isChangeDetectionActive() {
        return changeDetectionActive;
    }

    public void setChangeDetectionActive(boolean changeDetectionActive) {
        this.changeDetectionActive = changeDetectionActive;
    }

    public Long getTimeWindowMillis() {
        return timeWindowMillis;
    }

    public void setTimeWindowMillis(Long timeWindowMillis) {
        this.timeWindowMillis = timeWindowMillis;
    }
}