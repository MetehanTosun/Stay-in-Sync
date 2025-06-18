package de.unistuttgart.stayinsync.syncnode.logik_engine;

import jakarta.json.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class LogicGraphEvaluator {

    private final OperationCalculator calculator;

    public LogicGraphEvaluator() {
        this.calculator = new OperationCalculator();
    }

    /**
     * Evaluates the given list of {@link LogicNode}s, which form a logic graph.
     * This method assumes the graph has been pre-validated according to specific criteria:
     * - It must be a Directed Acyclic Graph (DAG).
     * - All {@link LogicOperator}s must have their inputs correctly defined regarding type and arity.
     * - All {@link InputNode}s (like {@link JsonNode}, {@link ConstantNode}) must be able to provide their values.
     * - There must be exactly one target node (a node with no children/outgoing dependencies to other LogicNodes).
     * - This single target node must produce a {@link Boolean} result.
     *
     * @param allNodesInGraph A list containing all {@link LogicNode}s that constitute the graph to be evaluated.
     * @param dataContext     A map containing the runtime data sources required by {@link JsonNode}s within the graph.
     *                        The map's key is the {@code sourceName} defined in a {@code JsonNode}, and the value
     *                        is the corresponding {@link JsonObject} to be queried. Can be an empty map if no
     *                        JsonNodes are used.
     * @return {@code true} or {@code false} representing the final evaluated boolean state of the graph.
     * @throws IllegalArgumentException If {@code allNodesInGraph} is null or empty.
     * @throws IllegalStateException    if the graph processing fails, e.g., a required data source is missing
     *                                  from the context or a JSON path is not found.
     */
    public boolean evaluateGraph(List<LogicNode> allNodesInGraph,  Map<String, JsonObject> dataContext) throws IllegalArgumentException, IllegalStateException {

        if (allNodesInGraph == null || allNodesInGraph.isEmpty()) {
            throw new IllegalArgumentException("The list of graph nodes cannot be null or empty");
        }

        // Step 1: Initialize helper data structures
        Map<LogicNode, Integer> nodeInDegree = new HashMap<>();
        Map<LogicNode, List<LogicNode>> childrenList = new HashMap<>();
        Queue<LogicNode> readyToEvaluateQueue = new LinkedList<>();

        // Step 1a: Prepare maps for each node and reset results
        for (int i = 0; i < allNodesInGraph.size(); i++) {
            LogicNode node = allNodesInGraph.get(i);
            nodeInDegree.put(node, 0); // In-degree will be set correctly in Step 2
            childrenList.put(node, new ArrayList<LogicNode>()); // Create an empty children list
            if (node.getCalculatedResult() != null) {
                node.setCalculatedResult(null); // Reset any old result in the node
            }
        }

        // Step 2: Analyze graph structure - calculate in-degrees and populate children lists
        // Determines which nodes depend on others and which can start immediately.
        for (int i = 0; i < allNodesInGraph.size(); i++) {
            LogicNode currentNode = allNodesInGraph.get(i);
            int parentNodeDependencies = 0;
            if (currentNode.getInputProviders() != null) {
                for (int j = 0; j < currentNode.getInputProviders().size(); j++) {
                    InputNode provider = currentNode.getInputProviders().get(j);
                    if (provider.isParentNode()) {
                        LogicNode parentNode = provider.getParentNode();
                        parentNodeDependencies++;
                        childrenList.get(parentNode).add(currentNode);
                    }
                }
            }
            nodeInDegree.put(currentNode, parentNodeDependencies);  // Store the actual in-degree
            if (parentNodeDependencies == 0) {
                readyToEvaluateQueue.add(currentNode);
            }
        }

        // --- MAIN EVALUATION LOOP ---

        // Step 3: Evaluate nodes in topological order
        // Processes nodes from the 'readyToEvaluateQueue' until it's empty.
        while (!readyToEvaluateQueue.isEmpty()) {
            LogicNode nodeToEvaluate = readyToEvaluateQueue.poll();

            // Step 3a: Gather inputs for the current node
            List<Object> valuesForOperation = new ArrayList<>();

            if (nodeToEvaluate.getInputProviders() != null) {
                for (InputNode provider : nodeToEvaluate.getInputProviders()) {
                    valuesForOperation.add(provider.getValue(dataContext));
                }
            }

            // Step 3b: Execute the node's operation
            Object  result = calculator.calculate(nodeToEvaluate.getOperator(), valuesForOperation);

            // Step 3c: Store the result
            nodeToEvaluate.setCalculatedResult(result);

            // Step 3d: Update in-degree of children and add to queue if ready
            List<LogicNode> children = childrenList.get(nodeToEvaluate);
            for (LogicNode childNode : children) {
                int newInDegree = nodeInDegree.get(childNode) - 1;
                nodeInDegree.put(childNode, newInDegree);
                if (newInDegree == 0) {
                    readyToEvaluateQueue.add(childNode);
                }
            }
        } // End of evaluation loop

        // --- DETERMINE RESULT ---

        // Step 4: Find the implicit target node (node with no children).
        LogicNode finalTargetNode = null;
        for (LogicNode node : allNodesInGraph) {
            List<LogicNode> children = childrenList.get(node);
            if (children.isEmpty()) {
                finalTargetNode = node;
                break;
            }
        }

        return (boolean) finalTargetNode.getCalculatedResult();

    }
}