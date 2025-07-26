package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

/**
 * Represents the static metadata for a single logic operator, persisted in the database.
 * This entity defines the "signature" of an operator for consumption by a frontend UI.
 */
@Entity
public class OperatorMetadata extends PanacheEntityBase {

    /**
     * The unique name of the operator, which also serves as the primary key.
     * This name corresponds to a {@link de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.LogicOperator} enum constant.
     * Example: "ADD", "EQUALS".
     */
    @Id
    @Column(name = "operator_name")
    public String operatorName;

    /**
     * A human-readable description of what the operator does for UI tooltips.
     */
    @Lob
    public String description;

    /**
     * The category this operator belongs to (e.g., "ARRAY", "STRING", "GENERAL").
     * Used for grouping in the UI.
     */
    @Column(name = "category")
    public String category;

    /**
     * A JSON string representing an array of expected input types (e.g., "[\"NUMBER\", \"NUMBER\"]").
     */
    @Lob
    @Column(name = "input_types")
    public String inputTypesJson;

    /**
     * A string representing the output type of the operator (e.g., "NUMBER", "BOOLEAN").
     */
    @Column(name = "output_type")
    public String outputType;
}