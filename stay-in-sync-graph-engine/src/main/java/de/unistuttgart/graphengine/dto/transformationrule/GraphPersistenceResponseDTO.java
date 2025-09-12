package de.unistuttgart.graphengine.dto.transformationrule;

import de.unistuttgart.graphengine.validation_error.ValidationError;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * A Data Transfer Object representing the result of a graph persistence operation (create or update).
 * It contains the graph's data and a list of any validation errors that occurred.
 */
@Getter
@Setter
public class GraphPersistenceResponseDTO {

    /**
     * The full DTO of the saved graph.
     */
    private GraphDTO graph;

    /**
     * A list of validation errors. This list is empty if the graph is valid and finalized.
     */
    private List<ValidationError> errors;

    public GraphPersistenceResponseDTO() {
    }

    public GraphPersistenceResponseDTO(GraphDTO graph, List<ValidationError> errors) {
        this.graph = graph;
        this.errors = errors;
    }
}
