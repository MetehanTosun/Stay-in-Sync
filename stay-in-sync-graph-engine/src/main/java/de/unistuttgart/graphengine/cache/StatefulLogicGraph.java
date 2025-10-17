package de.unistuttgart.graphengine.cache;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.graphengine.exception.GraphConstructionException;
import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.logic_engine.LogicGraphEvaluator;
import de.unistuttgart.graphengine.nodes.ConfigNode;
import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.graphengine.nodes.SnapshotEntry;
import io.quarkus.logging.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a stateful instance of a transformation rule's logic graph.
 * <p>
 * This class holds the parsed graph definition and the last known snapshot ("its memory")
 * between executions, making the change detection process highly efficient.
 * <p>
 * Each instance is immutable after construction and thread-safe for the stored graph definition,
 * but maintains mutable state for the snapshot between evaluations.
 */
public class StatefulLogicGraph {

    private final List<Node> graphDefinition;
    private final ConfigNode configNode;
    private Map<String, SnapshotEntry> lastSnapshot;
    private final LogicGraphEvaluator evaluator;

    /**
     * Constructs a new stateful graph instance.
     * <p>
     * It immediately validates the graph and locates the ConfigNode for efficient access.
     *
     * @param graphDefinition The list of nodes representing the graph. Must not be null or empty.
     * @throws GraphConstructionException if the graph definition is null, empty, or structurally invalid.
     */
    public StatefulLogicGraph(List<Node> graphDefinition) {
        if (graphDefinition == null) {
            throw new GraphConstructionException(
                GraphConstructionException.ErrorType.NULL_INPUT,
                "Graph definition cannot be null. A valid graph must contain at least a ConfigNode and a FinalNode."
            );
        }
        
        if (graphDefinition.isEmpty()) {
            throw new GraphConstructionException(
                GraphConstructionException.ErrorType.EMPTY_GRAPH,
                "Graph definition cannot be empty. A valid graph must contain at least a ConfigNode and a FinalNode."
            );
        }

        // Store immutable copy to prevent external modifications
        this.graphDefinition = List.copyOf(graphDefinition);
        this.lastSnapshot = new HashMap<>(); // Start with an empty memory
        this.evaluator = new LogicGraphEvaluator();

        // Find and validate ConfigNode presence
        this.configNode = findConfigNode(graphDefinition);
        
        Log.debugf("Successfully initialized StatefulLogicGraph");
    }

    /**
     * Executes the graph logic with the new live data against its stored snapshot.
     * <p>
     * After execution, it updates its internal snapshot for the next run, enabling
     * efficient change detection across multiple evaluations.
     *
     * @param sourceData The current data context from the source system.
     * @return The boolean result of the graph evaluation.
     * @throws GraphEvaluationException if a runtime error occurs during graph processing.
     * @throws GraphConstructionException if sourceData is null.
     */
    public boolean evaluate(Map<String, JsonNode> sourceData) throws GraphEvaluationException {
        if (sourceData == null) {
            throw new GraphConstructionException(
                GraphConstructionException.ErrorType.NULL_INPUT,
                "Source data cannot be null"
            );
        }

        Log.tracef("Evaluating graph with %d source data entries and %d snapshot entries",
            sourceData.size(), lastSnapshot.size());

        // Prepare data context with source data and snapshot
        Map<String, Object> dataContext = new HashMap<>(sourceData);
        dataContext.put("__snapshot", this.lastSnapshot);

        // Evaluate the graph
        boolean finalResult = evaluator.evaluateGraph(this.graphDefinition, dataContext);

        // Update snapshot for next evaluation
        this.lastSnapshot = this.configNode.getNewSnapshotData();

        Log.tracef("Graph evaluation completed with result: %b. Snapshot updated with %d entries.",
            finalResult, lastSnapshot.size());

        return finalResult;
    }

    /**
     * Finds and validates the ConfigNode within the graph definition.
     * <p>
     * A valid graph must contain exactly one ConfigNode for change detection.
     *
     * @param nodes The list of nodes to search.
     * @return The ConfigNode found in the graph.
     * @throws GraphConstructionException if no ConfigNode or multiple ConfigNodes are found.
     */
    private ConfigNode findConfigNode(List<Node> nodes) {
        ConfigNode foundConfigNode = null;
        int configNodeCount = 0;

        for (Node node : nodes) {
            if (node instanceof ConfigNode) {
                foundConfigNode = (ConfigNode) node;
                configNodeCount++;
            }
        }

        if (configNodeCount == 0) {
            Log.errorf("Invalid graph structure: No ConfigNode found in graph with %d nodes", nodes.size());
            throw new GraphConstructionException(
                GraphConstructionException.ErrorType.MISSING_REQUIRED_NODE,
                "Invalid graph structure: No ConfigNode found. " +
                "Every graph must contain exactly one ConfigNode for change detection."
            );
        }

        if (configNodeCount > 1) {
            Log.errorf("Invalid graph structure: Found %d ConfigNodes in graph", configNodeCount);
            throw new GraphConstructionException(
                GraphConstructionException.ErrorType.DUPLICATE_NODE,
                String.format(
                    "Invalid graph structure: Found %d ConfigNodes, but exactly one is required. " +
                    "Multiple ConfigNodes would lead to ambiguous change detection behavior.",
                    configNodeCount
                )
            );
        }

        return foundConfigNode;
    }

    /**
     * Returns the number of nodes in this graph.
     * Useful for debugging and monitoring.
     *
     * @return The total number of nodes in the graph definition.
     */
    public int getNodeCount() {
        return graphDefinition.size();
    }

    /**
     * Returns the current snapshot size.
     * Useful for debugging and monitoring change detection state.
     *
     * @return The number of entries in the current snapshot.
     */
    public int getSnapshotSize() {
        return lastSnapshot.size();
    }

    /**
     * Returns the ConfigNode's name for debugging purposes.
     *
     * @return The name of the ConfigNode in this graph.
     */
    public String getConfigNodeName() {
        return configNode.getName();
    }
}
