package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import de.unistuttgart.stayinsync.transport.transformation_rule_shared.GraphStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

/**
 * Represents the main entity for a transformation rule.
 * It holds metadata like the name and description, and maintains a one-to-one
 * relationship with the LogicGraphEntity that contains the actual graph definition.
 */
@Entity
public class TransformationRule extends PanacheEntity {

    /**
     * A user-defined, unique name for the transformation rule.
     */
    @Column(nullable = false, unique = true)
    public String name;

    @OneToOne
    public Transformation transformation;

    /**
     * An optional, human-readable description of what the rule does.
     */
    @Lob
    public String description;

    /**
     * The validation status of the associated graph (DRAFT or FINALIZED).
     */
    @Enumerated(EnumType.STRING)
    public GraphStatus graphStatus = GraphStatus.DRAFT;

    /**
     * A JSON string representing a list of {@link de.unistuttgart.stayinsync.transport.transformation_rule_shared.validation_error.ValidationError} objects.
     * <p>
     * This field is only populated if the {@code graphStatus} is {@code DRAFT}.
     * It stores the specific reasons why the graph failed validation, allowing the
     * frontend to display detailed, actionable feedback to the user.
     */
    @Lob
    @Column(name = "validation_errors_json", columnDefinition = "LONGTEXT")
    public String validationErrorsJson;

    /**
     * The one-to-one relationship to the entity holding the graph's JSON definition.
     */
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public LogicGraphEntity graph;

    public int updateIntervall;

}
