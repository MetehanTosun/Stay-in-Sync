package de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow;

import de.unistuttgart.stayinsync.transport.transformation_rule_shared.validation_error.ValidationError;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * A DTO representing the response for a GET request to a specific graph endpoint.
 * It contains the full visual graph definition (VFlow format) and any associated
 * validation errors.
 */
@Getter
@Setter
public class VflowGraphResponseDTO {

    private List<VFlowNodeDTO> nodes;
    private List<VFlowEdgeDTO> edges;
    private List<ValidationError> errors;

    public VflowGraphResponseDTO(List<VFlowNodeDTO> nodes, List<VFlowEdgeDTO> edges, List<ValidationError> errors) {
        this.nodes = nodes;
        this.edges = edges;
        this.errors = errors;
    }

}
