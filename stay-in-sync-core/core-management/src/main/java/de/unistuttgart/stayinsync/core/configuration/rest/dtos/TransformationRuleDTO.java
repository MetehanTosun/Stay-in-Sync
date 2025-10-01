package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.graphengine.validation_error.GraphStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * A Data Transfer Object representing the metadata of a TransformationRule.
 * Used for list views where the full graph definition is not needed.
 */
@Getter
@Setter
@AllArgsConstructor
public class TransformationRuleDTO {

    /**
     * The unique database ID of the rule.
     */
    private Long id;

    /**
     * The user-defined name of the rule.
     */
    private String name;

    /**
     * An optional description of what the rule does.
     */
    private String description;

    /**
     * The validation status of the associated graph (DRAFT or FINALIZED).
     */
    private GraphStatus graphStatus;

    /**
     * The ID of the parent transformation this rule belongs to.
     */
    private Long transformationId;

    public TransformationRuleDTO() {}
}
