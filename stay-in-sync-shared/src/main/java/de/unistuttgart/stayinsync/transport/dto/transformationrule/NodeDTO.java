package de.unistuttgart.stayinsync.transport.dto.transformationrule;

import java.util.List;

/**
 * A "flat" Data Transfer Object for a single node in the logic graph.
 * It contains all possible properties for any node type. The 'nodeType' field
 * acts as a discriminator to determine which properties are relevant.
 */

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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<InputDTO> getInputNodes() {
        return inputNodes;
    }

    public void setInputNodes(List<InputDTO> inputNodes) {
        this.inputNodes = inputNodes;
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
}