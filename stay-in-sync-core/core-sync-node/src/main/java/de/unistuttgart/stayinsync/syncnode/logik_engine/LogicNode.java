package de.unistuttgart.stayinsync.syncnode.logik_engine;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.LogicOperator;
import de.unistuttgart.stayinsync.syncnode.logik_engine.LogicOperator.Operation;
import jakarta.json.JsonObject;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class LogicNode {

    // Using String 'nodeName' as the primary identifier for simplicity and readability in graph definitions
    // and debugging. It's crucial that these names are unique within a given graph instance for
    // correct behavior in HashMaps and for target node identification.
    // An alternative could be a system-generated ID (long/UUID) if name management becomes an issue.
    private final  String nodeName;
    private final LogicOperator operator;
    private List<InputNode> inputProviders;
    private Object calculatedResult;


    public LogicNode(String nodeName, LogicOperator operator, InputNode... providers) {
        this.nodeName = nodeName;
        this.operator = operator;
        this.inputProviders = Arrays.asList(providers);
    }


    /**
     * Constructor used by mappers during the first phase of graph deserialization.
     * It creates a node instance without its connections (inputs).
     *
     * @param nodeName A unique name for the node within the graph.
     * @param operator The logical operation this node performs.
     */
    public LogicNode(String nodeName, LogicOperator operator) {
        if (nodeName == null || nodeName.trim().isEmpty()) {
            throw new IllegalArgumentException("Node name cannot be null or empty.");
        }
        if (operator == null) {
            throw new IllegalArgumentException("Operator cannot be null.");
        }
        this.nodeName = nodeName;
        this.operator = operator;
        this.inputProviders = new ArrayList<>(); // Initialize with an empty list to avoid NullPointerExceptions
    }


    /**
     * Triggers the evaluation of this node and stores the result internally.
     * <p>
     * This method is the primary entry point for the node's self-evaluation.
     * It uses the Strategy Pattern to delegate the
     * specific validation and execution logic to the appropriate {@link Operation}
     * implementation defined by this node's operator.
     * <p>
     * If the node is not configured correctly for its operator, the validation
     * step will throw an {@link IllegalArgumentException}.
     *
     * @param dataContext The runtime data context, required for operations that
     *                    interact with external JSON data sources.
     */
    public void calculate(Map<String, JsonNode> dataContext) {
        // 1. Retrieve the specific strategy implementation from the operator.
        Operation strategy = this.operator.getOperationStrategy();

        // 2. The node hands itself over to the strategy for configuration validation.
        strategy.validate(this);

        // 3. Delegate the actual execution to the strategy, passing itself and the context.
        Object result = strategy.execute(this, dataContext);

        // 4. Store the computed result internally, making it available for child nodes.
        this.setCalculatedResult(result);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two {@code LogicNode} instances are considered equal if their {@code nodeName}s are equal.
     *
     * @param o The reference object with which to compare.
     * @return {@code true} if this object is the same as the obj argument;
     *         {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogicNode logicNode = (LogicNode) o;
        return nodeName.equals(logicNode.nodeName);
    }


    /**
     * Returns a hash code value for the object. This method is
     * supported for the benefit of hash tables such as those provided by
     * {@link java.util.HashMap}.
     * The hash code is based on the {@code nodeName}.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(nodeName);
    }

}
