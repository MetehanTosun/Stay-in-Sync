package de.unistuttgart.stayinsync.syncnode.logik_engine;

import jakarta.json.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

public class LogicGraphEvaluator {

    private final OperationCalculator calculator;
    private final JsonObjectValueExtractor jsonValueExtractor;

    public LogicGraphEvaluator() {
        this.calculator = new OperationCalculator();
        this.jsonValueExtractor = new JsonObjectValueExtractor();
    }

    /**
     * Evaluates a graph of LogicNodes. The target node is implicitly the node
     * that has no outgoing edges to other LogicNodes (no children).
     * It is expected that there is exactly one such node.
     *
     * @param allNodesInGraph The list of all nodes in the graph. Node names must be unique.
     * @param rootJsonObject  The JsonObject containing external AAS input values
     * @param uiValues        A Map containing values from UI elements
     * @return The computed result of the implicit target node.
     * @throws IllegalArgumentException If input parameters are invalid.
     * @throws IllegalStateException    If the graph contains a cycle, no or multiple target nodes are found,
     *                                  a required value is not found, or other evaluation errors occur.
     */
    public Object evaluateGraph(List<LogicNode> allNodesInGraph, JsonObject rootJsonObject, Map<String, Object> uiValues)

            throws IllegalArgumentException, IllegalStateException {

        // Step 0: Input Validations
        // Ensures that the basic parameters are valid.
        if (allNodesInGraph == null || allNodesInGraph.isEmpty()) {
            throw new IllegalArgumentException("The list of graph nodes cannot be null or empty.");
        }
        if (uiValues == null) {
            throw new IllegalArgumentException("The uiValues map cannot be null (can be empty).");
        }

        // Helper data structures
        Map<LogicNode, Integer> nodeInDegree = new HashMap<>();
        Map<LogicNode, List<LogicNode>> nodeToChildrenMap = new HashMap<>();
        Map<LogicNode, Object> computedNodeResults = new HashMap<>();
        Queue<LogicNode> readyToEvaluateQueue = new LinkedList<>();
        Map<String, LogicNode> nameCheckMap = new HashMap<>();

        // Step 1: Initialize helper structures and check for duplicate node names
        // Prepares internal maps and validates node name uniqueness.
        for (int i = 0; i < allNodesInGraph.size(); i++) {
            LogicNode node = allNodesInGraph.get(i);
            if (node.getNodeName() == null || node.getNodeName().trim().isEmpty()) {
                throw new IllegalArgumentException("A node in the graph has a null or empty name.");
            }
            if (nameCheckMap.containsKey(node.getNodeName())) {
                throw new IllegalArgumentException("Duplicate node name found in graph: " + node.getNodeName());
            }
            nameCheckMap.put(node.getNodeName(), node);
            nodeInDegree.put(node, 0);
            nodeToChildrenMap.put(node, new ArrayList<LogicNode>());
            if (node.getCalculatedResult() != null) {
                node.setCalculatedResult(null);
            }
        }

        // Step 2: Analyze graph structure - build in-degrees and children relationships
        // Determines dependencies between nodes and identifies initial nodes for evaluation.
        for (int i = 0; i < allNodesInGraph.size(); i++) {
            LogicNode currentNode = allNodesInGraph.get(i);
            int parentNodeDependencies = 0;
            if (currentNode.getInputProviders() != null) {
                for (int j = 0; j < currentNode.getInputProviders().size(); j++) {
                    InputProvider provider = currentNode.getInputProviders().get(j);
                    if (provider.isNodeSource()) {
                        LogicNode parentNode = provider.getParentNode();
                        parentNodeDependencies++;
                        List<LogicNode> childrenOfParent = nodeToChildrenMap.get(parentNode);
                        if (childrenOfParent != null) {
                            childrenOfParent.add(currentNode);
                        } else {
                            throw new IllegalStateException("Internal error: Children list for parent node '" + parentNode.getNodeName() + "' not initialized.");
                        }
                    }
                }
            }
            nodeInDegree.put(currentNode, parentNodeDependencies);
            if (parentNodeDependencies == 0) {
                readyToEvaluateQueue.add(currentNode);
            }
        }

        // Step 3: Main evaluation loop
        // Processes nodes in topological order.
        int evaluatedNodesCount = 0;
        while (!readyToEvaluateQueue.isEmpty()) {
            LogicNode nodeToEvaluate = readyToEvaluateQueue.poll();
            evaluatedNodesCount++;
            List<Object> valuesForOperation = new ArrayList<>();
            if (nodeToEvaluate.getInputProviders() != null) {
                for (int i = 0; i < nodeToEvaluate.getInputProviders().size(); i++) {
                    InputProvider provider = nodeToEvaluate.getInputProviders().get(i);
                    if (provider.isExternalSource()) {
                        if (rootJsonObject == null) {
                            throw new IllegalStateException("Node '" + nodeToEvaluate.getNodeName() + "' requires an external JSON input (AAS), but no rootJsonObject was provided.");
                        }
                        String jsonPath = provider.getExternalJsonPath();
                        Optional<Object> optionalValue = jsonValueExtractor.extractValue(rootJsonObject, jsonPath);
                        if (optionalValue.isPresent()) {
                            valuesForOperation.add(optionalValue.get());
                        } else {
                            throw new IllegalStateException("Missing or null value in JSON for path '" + jsonPath +
                                    "' (required by node '" + nodeToEvaluate.getNodeName() + "').");
                        }
                    } else if (provider.isNodeSource()) {
                        LogicNode parentNode = provider.getParentNode();
                        if (computedNodeResults.containsKey(parentNode)) {
                            valuesForOperation.add(computedNodeResults.get(parentNode));
                        } else {
                            throw new IllegalStateException("Result for parent node '" + parentNode.getNodeName() +
                                    "' not found (required by node '" +
                                    nodeToEvaluate.getNodeName() + "'). Cycle or algorithm error.");
                        }
                    } else if (provider.isUISource()) {
                        String uiElementName = provider.getUiElementName();
                        if (uiValues.containsKey(uiElementName)) {
                            Object uiValue = uiValues.get(uiElementName);
                            if (uiValue == null) {
                                throw new IllegalStateException("Value for UI element '" + uiElementName + "' is null (required by node '" + nodeToEvaluate.getNodeName() + "').");
                            }
                            valuesForOperation.add(uiValue);
                        } else {
                            throw new IllegalStateException("Missing value for UI element '" + uiElementName +
                                    "' in uiValues map (required by node '" + nodeToEvaluate.getNodeName() + "').");
                        }
                    } else {
                        throw new IllegalStateException("Unknown InputProvider type for node '" + nodeToEvaluate.getNodeName() + "'.");
                    }
                }
            }
            Object result;
            try {
                result = calculator.calculate(nodeToEvaluate.getOperator(), valuesForOperation);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Error during calculation of node '" + nodeToEvaluate.getNodeName() + "': " + e.getMessage(), e);
            } catch (ArithmeticException e) {
                throw new IllegalStateException("Arithmetic error at node '" + nodeToEvaluate.getNodeName() + "': " + e.getMessage(), e);
            }
            nodeToEvaluate.setCalculatedResult(result);
            computedNodeResults.put(nodeToEvaluate, result);
            List<LogicNode> children = nodeToChildrenMap.get(nodeToEvaluate);
            for (int i = 0; i < children.size(); i++) {
                LogicNode childNode = children.get(i);
                Integer currentInDegree = nodeInDegree.get(childNode);
                if (currentInDegree == null) {
                    throw new IllegalStateException("Internal error: In-degree for child node '" + childNode.getNodeName() + "' not found.");
                }
                int newInDegree = currentInDegree - 1;
                nodeInDegree.put(childNode, newInDegree);
                if (newInDegree == 0) {
                    readyToEvaluateQueue.add(childNode);
                }
            }
        }

        // Step 4: Check for cycles or unresolvable parts of the graph
        // Ensures all nodes were processed if the graph is a valid DAG.
        if (evaluatedNodesCount < allNodesInGraph.size()) {
            String unEvaluatedNodeNames = "";
            for (int i = 0; i < allNodesInGraph.size(); i++) {
                LogicNode node = allNodesInGraph.get(i);
                if (!computedNodeResults.containsKey(node)) {
                    if (!unEvaluatedNodeNames.isEmpty()) {
                        unEvaluatedNodeNames = unEvaluatedNodeNames + ", ";
                    }
                    unEvaluatedNodeNames = unEvaluatedNodeNames + node.getNodeName();
                }
            }
            throw new IllegalStateException("Cycle detected in graph or graph is not fully evaluable. " +
                    "Unevaluated nodes: " + unEvaluatedNodeNames);
        }

        // Step 5: Find the target node (node with no children) and return its result
        // Identifies the implicit output node of the graph.
        List<LogicNode> potentialTargetNodes = new ArrayList<>();
        for (int i = 0; i < allNodesInGraph.size(); i++) {
            LogicNode node = allNodesInGraph.get(i);
            if (nodeToChildrenMap.get(node).isEmpty()) {
                potentialTargetNodes.add(node);
            }
        }

        if (potentialTargetNodes.isEmpty()) {
            if (!allNodesInGraph.isEmpty()) {
                throw new IllegalStateException("No target node (node without children) found in the graph.");
            } else {
                throw new IllegalArgumentException("The provided graph is empty; no result can be determined.");
            }
        }
        if (potentialTargetNodes.size() > 1) {
            String targetNodeNames = "";
            for(int i = 0; i < potentialTargetNodes.size(); i++){
                if(i > 0) {
                    targetNodeNames = targetNodeNames + ", ";
                }
                targetNodeNames = targetNodeNames + potentialTargetNodes.get(i).getNodeName();
            }
            throw new IllegalStateException("Multiple target nodes (nodes without children) found: " + targetNodeNames +
                    ". The logic engine expects a single unique end node.");
        }

        LogicNode finalTargetNode = potentialTargetNodes.get(0);

        if (!computedNodeResults.containsKey(finalTargetNode)) {
            throw new IllegalStateException("Result for target node '" + finalTargetNode.getNodeName() +
                    "' could not be computed (unexpected after cycle check).");
        }
        return computedNodeResults.get(finalTargetNode);
    }
}