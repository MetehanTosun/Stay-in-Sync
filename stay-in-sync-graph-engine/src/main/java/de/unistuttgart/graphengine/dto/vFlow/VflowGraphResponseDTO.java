package de.unistuttgart.graphengine.dto.vFlow;



import de.unistuttgart.graphengine.validation_error.ValidationError;


import java.util.List;

/**
 * A DTO representing the response for a GET request to a specific graph endpoint.
 * It contains the full visual graph definition (VFlow format) and any associated
 * validation errors.
 */

public class VflowGraphResponseDTO {

    private List<VFlowNodeDTO> nodes;
    private List<VFlowEdgeDTO> edges;
    private List<ValidationError> errors;

    public VflowGraphResponseDTO(List<VFlowNodeDTO> nodes, List<VFlowEdgeDTO> edges, List<ValidationError> errors) {
        this.nodes = nodes;
        this.edges = edges;
        this.errors = errors;
    }

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

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }
}
