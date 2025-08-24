package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.transport.transformation_rule_shared.GraphStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * A Data Transfer Object representing the metadata of a TransformationRule.
 * Used for list views where the full graph definition is not needed.
 */
@Getter
@Setter
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GraphStatus getGraphStatus() {
        return graphStatus;
    }

    public void setGraphStatus(GraphStatus graphStatus) {
        this.graphStatus = graphStatus;
    }

    public Long getTransformationId() {
        return transformationId;
    }

    public void setTransformationId(Long transformationId) {
        this.transformationId = transformationId;
    }
}
