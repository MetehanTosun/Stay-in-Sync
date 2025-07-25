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
@Getter
@Setter
public class LogicGraphEntity extends PanacheEntity {

    /**
     * The complete graph definition, stored as a JSON string in a TEXT or CLOB column.
     * This field contains the serialized version of a {@link GraphDTO}.
     */
    @Lob
    @Column(name = "graph_definition_json", columnDefinition = "TEXT", nullable = false)
    public String graphDefinitionJson;

}
