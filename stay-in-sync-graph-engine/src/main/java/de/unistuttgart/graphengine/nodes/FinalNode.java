package de.unistuttgart.graphengine.nodes;

import java.util.Map;

/**
 * A special node that represents the final output of the logic graph.
 * Every valid graph must contain exactly one FinalNode.
 * It provides the final boolean result of the entire graph evaluation.
 */

public class FinalNode extends Node {

    /**
     * Executes the calculation for this FinalNode.
     * <p>
     * The logic is simple:
     * <ul>
     * <li>If an input node is connected, this node's result is the
     * pre-calculated result of that single input.</li>
     * <li>If no input node is connected (e.g., in a default graph),
     * the result defaults to {@code true}.</li>
     * </ul>
     *
     * @param dataContext The runtime data context, which is not directly used by this node.
     */
    @Override
    public void calculate(Map<String, Object> dataContext) {
        // Check if an input node is connected
        if (this.getInputNodes() != null && !this.getInputNodes().isEmpty()) {
            // Pass through the result from the single input node.
            // A validation rule should ensure there is only one input.
            Object inputValue = this.getInputNodes().get(0).getCalculatedResult();
            this.setCalculatedResult(inputValue);
        } else {
            // Default value for a new, unconnected graph.
            this.setCalculatedResult(true);
        }
    }

    @Override
    public Class<?> getOutputType() {
        return Boolean.class;
    }
}