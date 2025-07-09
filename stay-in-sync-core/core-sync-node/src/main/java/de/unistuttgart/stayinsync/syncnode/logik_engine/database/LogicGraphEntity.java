package de.unistuttgart.stayinsync.syncnode.logik_engine.database;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Represents a persisted logic graph in the database.
 * This Panache entity maps to the 'logic_graphs' table.
 */
@Entity
@Table(name = "logic_graphs")
public class LogicGraphEntity extends PanacheEntity {

    /**
     * A user-defined, unique name for the logic graph (e.g., "SystemReadyCheck").
     * This can be used to easily find and load a specific graph.
     */
    @Column(nullable = false, unique = true)
    public String name;

    /**
     * The complete graph definition, stored as a JSON string in a TEXT or CLOB column.
     * This field contains the serialized version of a {@link de.unistuttgart.stayinsync.syncnode.logik_engine.database.DTOs.GraphDTO}.
     */
    @Lob
    @Column(name = "graph_definition_json", columnDefinition = "TEXT", nullable = false)
    public String graphDefinitionJson;
}
