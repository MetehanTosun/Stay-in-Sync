package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;

import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;

/**
 * A utility class that performs a topological sort on a graph of Nodes
 * and detects the presence and members of cycles.
 */
@ApplicationScoped
public class GraphTopologicalSorter {

    /**
     * A record to hold the result of the sorting process, containing the
     * sorted list, a cycle flag, and the IDs of nodes within the cycle.
     */
    public record SortResult(List<Node> sortedNodes, boolean hasCycle, List<Integer> cycleNodeIds) {}

    /**
     * Sorts a given graph of nodes topologically using Kahn's algorithm.
     * If a cycle is detected, it identifies the nodes that are part of it.
     *
     * @param graphNodes The list of all nodes in the graph.
     * @return A {@link SortResult} containing the sorted list, a cycle flag, and cycle node IDs.
     */
    public SortResult sort(List<Node> graphNodes) {
        Map<Node, Integer> nodeInDegree = new HashMap<>();
        Map<Node, List<Node>> childrenList = new HashMap<>();
        Queue<Node> queue = new LinkedList<>();
        List<Node> sortedList = new ArrayList<>();

        // Initialization
        for (Node node : graphNodes) {
            nodeInDegree.put(node, 0);
            childrenList.put(node, new ArrayList<>());
        }

        // Dependency Analysis
        for (Node currentNode : graphNodes) {
            if (currentNode.getInputNodes() != null) {
                nodeInDegree.put(currentNode, currentNode.getInputNodes().size());
                for (Node parentNode : currentNode.getInputNodes()) {
                    if (childrenList.containsKey(parentNode)) {
                        childrenList.get(parentNode).add(currentNode);
                    }
                }
            }
        }

        // Find Initial Nodes
        for (Map.Entry<Node, Integer> entry : nodeInDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        // Perform Topological Sort
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            sortedList.add(node);
            for (Node child : childrenList.get(node)) {
                int newInDegree = nodeInDegree.get(child) - 1;
                nodeInDegree.put(child, newInDegree);
                if (newInDegree == 0) {
                    queue.add(child);
                }
            }
        }

        // --- Cycle Detection & Node Identification ---
        boolean hasCycle = sortedList.size() != graphNodes.size();
        List<Integer> cycleNodeIds = new ArrayList<>();

        if (hasCycle) {
            // If a cycle exists, any node with a remaining in-degree > 0 is part of it.
            for (Map.Entry<Node, Integer> entry : nodeInDegree.entrySet()) {
                if (entry.getValue() > 0) {
                    cycleNodeIds.add(entry.getKey().getId());
                }
            }
        }

        return new SortResult(sortedList, hasCycle, cycleNodeIds);
    }
}