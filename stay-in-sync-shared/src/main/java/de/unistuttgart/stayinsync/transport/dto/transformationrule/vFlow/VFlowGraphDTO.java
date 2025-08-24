package de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow;


import java.util.List;

/**
 * The top-level DTO representing a complete graph from the ngx-vflow frontend.
 * It contains a list of nodes and a list of edges.
 */

public class VFlowGraphDTO {


    /**
     * A list of all nodes in the graph.
     */
    private List<VFlowNodeDTO> nodes;

    /**
     * A list of all edges (connections) in the graph.
     */
    private List<VFlowEdgeDTO> edges;

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
}
