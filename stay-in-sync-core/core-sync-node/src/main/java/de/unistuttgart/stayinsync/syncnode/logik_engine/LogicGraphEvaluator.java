// Datei: LogicGraphEvaluator.java
package de.unistuttgart.stayinsync.syncnode.logik_engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
// Optional wird weiterhin vom jsonValueExtractor verwendet, aber wir rufen .get() direkter auf unter Annahme der Existenz
import java.util.Queue;

public class LogicGraphEvaluator {

    private final OperationCalculator calculator;

    public LogicGraphEvaluator() {
        this.calculator = new OperationCalculator();
    }


    public boolean evaluateGraph(List<LogicNode> allNodesInGraph) throws IllegalArgumentException, IllegalStateException {

        if (allNodesInGraph == null || allNodesInGraph.isEmpty()) {
            throw new IllegalArgumentException("The list of graph nodes cannot be null or empty");
        }

        // Step 1: Initialize helper data structures
        Map<LogicNode, Integer> nodeInDegree = new HashMap<>();
        Map<LogicNode, List<LogicNode>> childrenList = new HashMap<>();
        Map<LogicNode, Object> computedNodeResults = new HashMap<>();
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
                    if (provider instanceof JsonNode) {
                        valuesForOperation.add(((JsonNode) provider).getValue());
                    } else if (provider instanceof ParentNode) {
                        valuesForOperation.add(((ParentNode) provider).getValue());
                    } else if (provider instanceof ConstantNode) {
                        valuesForOperation.add(((ConstantNode) provider).getValue());
                    }
                }
            }

            // Step 3b: Execute the node's operation
            Object  result = calculator.calculate(nodeToEvaluate.getOperator(), valuesForOperation);

            // Step 3c: Store the result
            nodeToEvaluate.setCalculatedResult(result);
            computedNodeResults.put(nodeToEvaluate, result);

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

        return (boolean) computedNodeResults.get(finalTargetNode);

    }
}