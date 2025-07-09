package de.unistuttgart.stayinsync.syncnode.logik_engine;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;
import java.util.*;

/**
 * Evaluates a logic graph composed of Node objects.
 * The evaluation is performed using a topological sort algorithm (Kahn's algorithm).
 */
public class LogicGraphEvaluator {

    /**
     * Evaluates a given valid, directed acyclic graph (DAG) of {@link Node}s.
     * <p>
     * This method calculates the value for each node in a dependency-correct order
     * and returns the boolean result of the single target node. It presupposes
     * the graph is valid and contains no cycles.
     *
     * @param allNodesInGraph A non-empty list containing all {@link Node}s that constitute the graph.
     * @param dataContext     A map containing the runtime data sources required by ProviderNodes.
     * @return {@code true} or {@code false} representing the final evaluated state of the graph.
     * @throws IllegalArgumentException if the provided list of nodes is null or empty.
     *
     */
    public boolean evaluateGraph(List<Node> allNodesInGraph, Map<String, JsonNode> dataContext) {

        if (allNodesInGraph == null || allNodesInGraph.isEmpty()) {
            throw new IllegalArgumentException("The list of graph nodes cannot be null or empty");
        }

        // Step 1: Initialize helper data structures.
        Map<Node, Integer> nodeInDegree = new HashMap<>();
        Map<Node, List<Node>> childrenList = new HashMap<>();
        Queue<Node> readyToEvaluateQueue = new LinkedList<>();

        // Step 1a: Prepare maps for each node and reset any previous results.
        for (Node node : allNodesInGraph) {
            nodeInDegree.put(node, 0);
            childrenList.put(node, new ArrayList<>());
            node.setCalculatedResult(null);
        }

        // Step 2: Analyze graph structure to calculate in-degrees and populate children lists.
        for (Node currentNode : allNodesInGraph) {
            int parentNodeDependencies = 0;
            if (currentNode.getInputNodes() != null) {
                for (Node parentNode : currentNode.getInputNodes()) {
                    parentNodeDependencies++;
                    childrenList.get(parentNode).add(currentNode);
                }
            }
            nodeInDegree.put(currentNode, parentNodeDependencies);

            if (parentNodeDependencies == 0) {
                readyToEvaluateQueue.add(currentNode);
            }
        }

        // Step 3: Evaluate nodes in topological order.
        while (!readyToEvaluateQueue.isEmpty()) {
            Node nodeToEvaluate = readyToEvaluateQueue.poll();
            nodeToEvaluate.calculate(dataContext);

            for (Node childNode : childrenList.get(nodeToEvaluate)) {
                int newInDegree = nodeInDegree.get(childNode) - 1;
                nodeInDegree.put(childNode, newInDegree);

                if (newInDegree == 0) {
                    readyToEvaluateQueue.add(childNode);
                }
            }
        }

        // Step 4: Find the implicit target node (the node with no children).
        Node finalTargetNode = null;
        for (Node node : allNodesInGraph) {
            if (childrenList.get(node).isEmpty()) {
                finalTargetNode = node;
                break;
            }
        }

        return (boolean) finalTargetNode.getCalculatedResult();
    }
}