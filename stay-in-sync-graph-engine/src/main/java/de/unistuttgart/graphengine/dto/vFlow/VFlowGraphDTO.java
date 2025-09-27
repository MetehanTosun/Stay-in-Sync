package de.unistuttgart.graphengine.dto.vFlow;



import java.util.ArrayList;
import java.util.List;

/**
 * The top-level DTO representing a complete graph from the ngx-vflow frontend.
 * It contains a list of nodes and a list of edges.
 */

public class VFlowGraphDTO {

    public List<VFlowNodeDTO> getNodes() {
        return nodes;
    }

    public void setNodes(List<VFlowNodeDTO> nodes) {
        this.nodes = nodes;
    }

    public List<VFlowEdgeDTO> getEdges() {
        return edges;
    }

    public void setEdges(List<VFlowEdgeDTO> edges) {
        this.edges = edges;
    }

    /**
     * A list of all nodes in the graph.
     */
    private List<VFlowNodeDTO> nodes;

    /**
     * A list of all edges (connections) in the graph.
     */
    private List<VFlowEdgeDTO> edges;

    public VFlowGraphDTO() {
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
    }
}
