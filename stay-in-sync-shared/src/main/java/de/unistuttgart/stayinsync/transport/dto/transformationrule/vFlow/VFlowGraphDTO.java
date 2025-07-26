package de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * The top-level DTO representing a complete graph from the ngx-vflow frontend.
 * It contains a list of nodes and a list of edges.
 */
@Getter
@Setter
public class VFlowGraphDTO {


    /**
     * A list of all nodes in the graph.
     */
    private List<VFlowNodeDTO> nodes;

    /**
     * A list of all edges (connections) in the graph.
     */
    private List<VFlowEdgeDTO> edges;
}
