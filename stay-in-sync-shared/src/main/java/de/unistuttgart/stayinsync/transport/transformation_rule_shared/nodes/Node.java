package de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.transport.exception.GraphEvaluationException;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

/**
 * An abstract base class for all nodes in the logic graph.
 * It defines the common properties such as a unique ID, optional name,
 * visual position, and input connections.
 */
@Getter
@Setter
public abstract class Node {

    /**
     * The unique identifier for this node within its graph.
     */
    private int id;

    /**
     * An optional, human-readable name for the node (e.g., "TemperatureCheck").
     */
    private String name;

    /**
     * The visual x-coordinate for the node's position in a UI.
     * This is ignored by the engine during evaluation.
     */
    private double offsetX;

    /**
     * The visual y-coordinate for the node's position in a UI.
     * This is ignored by the engine during evaluation.
     */
    private double offsetY;

    /**
     * A list of parent nodes that provide input for this node's calculation.
     * This list is populated by the GraphMapper during graph creation and
     * represents the incoming edges to this node.
     */
    private List<Node> inputNodes;

    /**
     * Stores the result of this node's calculation once it has been evaluated.
     * Downstream nodes will read from this field.
     */
    private Object calculatedResult;

    /**
     * Triggers the evaluation of this node based on its specific logic.
     * Each concrete subclass must implement its own calculation behavior.
     * For nodes with inputs, this method is only called after all parent nodes
     * in the `inputNodes` list have been calculated.
     *
     * @param dataContext The runtime data context, used for resolving external
     * JSON values in ProviderNodes.
     */
    public abstract void calculate(Map<String, JsonNode> dataContext) throws GraphEvaluationException;

    public abstract Class<?> getOutputType();;
}