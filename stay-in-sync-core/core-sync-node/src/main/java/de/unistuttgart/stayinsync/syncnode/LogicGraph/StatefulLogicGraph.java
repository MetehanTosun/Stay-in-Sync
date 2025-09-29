package de.unistuttgart.stayinsync.syncnode.LogicGraph;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.logic_engine.LogicGraphEvaluator;
import de.unistuttgart.graphengine.nodes.ConfigNode;
import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.graphengine.nodes.SnapshotEntry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a stateful instance of a transformation rule's logic graph.
 * This class holds the parsed graph definition and the last known snapshot ("its memory")
 * between executions, making the change detection process highly efficient.
 */
public class StatefulLogicGraph {

    private final List<Node> graphDefinition;
    private final ConfigNode configNode;
    private Map<String, SnapshotEntry> lastSnapshot;
    private final LogicGraphEvaluator evaluator;

    /**
     * Constructs a new stateful graph instance.
     * It immediately finds and stores a reference to the graph's ConfigNode for efficient access.
     *
     * @param graphDefinition The list of nodes representing the graph.
     * @throws IllegalArgumentException if no ConfigNode is found in the graph definition.
     */
    public StatefulLogicGraph(List<Node> graphDefinition) {
        this.graphDefinition = graphDefinition;
        this.lastSnapshot = new HashMap<>(); // Start with an empty memory
        this.evaluator = new LogicGraphEvaluator();

        this.configNode = findConfigNodeOnce(graphDefinition);
    }

    /**
     * Executes the graph logic with the new live data against its stored snapshot.
     * After execution, it updates its internal snapshot for the next run.
     *
     * @param sourceData The current data context from the source system.
     * @return The boolean result of the graph evaluation.
     * @throws GraphEvaluationException if a runtime error occurs during graph processing.
     */
    public boolean evaluate(Map<String, JsonNode> sourceData) throws GraphEvaluationException {
        Map<String, Object> dataContext = new HashMap<>(sourceData);
        dataContext.put("__snapshot", this.lastSnapshot);

        boolean finalResult = evaluator.evaluateGraph(this.graphDefinition, dataContext);

        this.lastSnapshot = this.configNode.getNewSnapshotData();

        return finalResult;
    }

    /**
     * Finds the single ConfigNode within the graph definition using a for-loop.
     * This method assumes a valid graph where a ConfigNode is always present.
     */
    private ConfigNode findConfigNodeOnce(List<Node> nodes) {
        for (Node node : nodes) {
            if (node instanceof ConfigNode) {
                return (ConfigNode) node;
            }
        }
        return null;
    }
}