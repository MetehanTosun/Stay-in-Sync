package de.unistuttgart.stayinsync.transport.dto.transformationrule;

/**
 * A Data Transfer Object representing an edge in the graph.
 * It defines an input for a node by referencing the ID of the parent node.
 */

public class InputDTO {
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    /**
     * The unique ID of the node that provides the input.
     */
    private int id;

    /**
     * The zero-based index specifying the order of this input for the receiving node.
     */
    private int orderIndex;
}