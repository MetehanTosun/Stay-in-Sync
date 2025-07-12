package de.unistuttgart.stayinsync.syncnode.logik_engine.database;

import de.unistuttgart.stayinsync.syncnode.logik_engine.nodes.Node;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;

/**
 * A utility class that performs a topological sort on a graph of Nodes
 * and detects the presence of cycles.
 */
@ApplicationScoped
public class GraphTopologicalSorterService {

    /**
     * A record to hold the result of the sorting process, containing both the
     * sorted list of nodes and a flag indicating if a cycle was detected.
     */
    public record SortResult(List<Node> sortedNodes, boolean hasCycle) {}

    /**
     * Sorts a given graph of nodes topologically using Kahn's algorithm.
     *
     * @param graphNodes The list of all nodes in the graph.
     * @return A {@link SortResult} containing the sorted list and a boolean cycle flag.
     */
    public SortResult sort(List<Node> graphNodes) {
        Map<Node, Integer> nodeInDegree = new HashMap<>();
        Map<Node, List<Node>> childrenList = new HashMap<>();
        Queue<Node> queue = new LinkedList<>();
        List<Node> sortedList = new ArrayList<>();

        // --- Initialization ---
        for (Node node : graphNodes) {
            nodeInDegree.put(node, 0);
            childrenList.put(node, new ArrayList<>());
        }

        // --- Dependency Analysis ---
        // Calculate in-degrees and build the adjacency list (childrenList)
        for (Node currentNode : graphNodes) {
            if (currentNode.getInputNodes() != null) {
                nodeInDegree.put(currentNode, currentNode.getInputNodes().size());
                for (Node parentNode : currentNode.getInputNodes()) {
                    // This check ensures the parentNode exists in the map before adding a child
                    if (childrenList.containsKey(parentNode)) {
                        childrenList.get(parentNode).add(currentNode);
                    }
                }
            }
        }

        // --- Find Initial Nodes ---
        // Add all nodes with an in-degree of 0 to the queue
        for (Map.Entry<Node, Integer> entry : nodeInDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        // --- Perform Topological Sort ---
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            sortedList.add(node);

            // Update all children of the processed node
            for (Node child : childrenList.get(node)) {
                int newInDegree = nodeInDegree.get(child) - 1;
                nodeInDegree.put(child, newInDegree);
                if (newInDegree == 0) {
                    queue.add(child);
                }
            }
        }

        // --- Cycle Detection ---
        // If the sorted list contains fewer nodes than the original graph, a cycle exists.
        boolean hasCycle = sortedList.size() != graphNodes.size();

        return new SortResult(sortedList, hasCycle);
    }
}