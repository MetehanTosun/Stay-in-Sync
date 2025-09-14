package de.unistuttgart.graphengine.util;

import de.unistuttgart.graphengine.nodes.Node;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.*;

/**
 * A utility class that performs a topological sort on a graph of Nodes
 * and detects the presence and members of cycles.
 */
@ApplicationScoped
public class GraphTopologicalSorter {

    private static final Logger logger = Logger.getLogger(GraphTopologicalSorter.class);

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
        logger.debugf("Starting topological sort for a graph with %d nodes.", graphNodes.size());
        Map<Node, Integer> nodeInDegree = new HashMap<>();
        Map<Node, List<Node>> childrenList = new HashMap<>();
        Queue<Node> queue = new LinkedList<>();
        List<Node> sortedList = new ArrayList<>();

        // Initialization
        logger.debug("Pass 1: Initializing in-degree and adjacency maps.");
        for (Node node : graphNodes) {
            nodeInDegree.put(node, 0);
            childrenList.put(node, new ArrayList<>());
        }

        // Dependency Analysis
        logger.debug("Pass 2: Analyzing dependencies to populate in-degrees and children.");
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
        logger.debug("Pass 3: Finding initial nodes with an in-degree of 0.");
        for (Map.Entry<Node, Integer> entry : nodeInDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }
        logger.debugf("Found %d initial nodes to start the sort.", queue.size());

        // Perform Topological Sort
        logger.debug("Pass 4: Performing topological sort by processing the queue.");
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
        logger.debugf("Finished processing queue. Sorted list contains %d nodes.", sortedList.size());

        // --- Cycle Detection & Node Identification ---
        boolean hasCycle = sortedList.size() != graphNodes.size();
        List<Integer> cycleNodeIds = new ArrayList<>();

        if (hasCycle) {
            logger.warnf("Cycle detected in the graph! Total nodes: %d, sorted nodes: %d.", graphNodes.size(), sortedList.size());
            // If a cycle exists, any node with a remaining in-degree > 0 is part of it.
            for (Map.Entry<Node, Integer> entry : nodeInDegree.entrySet()) {
                if (entry.getValue() > 0) {
                    cycleNodeIds.add(entry.getKey().getId());
                }
            }
            logger.warnf("Identified %d nodes as part of the cycle: %s", cycleNodeIds.size(), cycleNodeIds);
        }

        logger.infof("Topological sort completed. Has cycle: %b. Sorted nodes: %d.", hasCycle, sortedList.size());
        return new SortResult(sortedList, hasCycle, cycleNodeIds);
    }
}