package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphDTO;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.GraphStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a persisted logic graph in the database.
 * This Panache entity maps to the 'logic_graphs' table.
 */
@Entity
@Table(name = "logic_graphs")
@Getter
@Setter
public class LogicGraphEntity extends PanacheEntity {

    /**
     * A user-defined, unique name for the logic graph (e.g., "SystemReadyCheck").
     * This can be used to easily find and load a specific graph.
     */
    @Column(nullable = false, unique = true)
    public String name;

    /**
     * The complete graph definition, stored as a JSON string in a TEXT or CLOB column.
     * This field contains the serialized version of a {@link GraphDTO}.
     */
    @Lob
    @Column(name = "graph_definition_json", columnDefinition = "TEXT", nullable = false)
    public String graphDefinitionJson;

    /**
     * The validation status of the graph, stored as a string ('DRAFT' or 'FINALIZED').
     * This flag indicates whether the graph has passed all validation checks and is
     * considered complete and ready for execution.
     */
    @Enumerated(EnumType.STRING)
    public GraphStatus status = GraphStatus.DRAFT;

    @Lob
    @Column(name = "validation_errors_json", columnDefinition = "LONGTEXT")
    public String validationErrorsJson;
}
