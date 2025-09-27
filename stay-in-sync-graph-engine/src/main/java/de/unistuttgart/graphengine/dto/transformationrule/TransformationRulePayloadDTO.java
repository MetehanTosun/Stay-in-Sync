package de.unistuttgart.graphengine.dto.transformationrule;


/**
 * A DTO that carries the full payload for creating or updating a TransformationRule.
 * It combines the metadata (name, description) with the full graph definition.
 */

public class TransformationRulePayloadDTO {

    /**
     * The user-defined name for the transformation rule.
     */
    private String name;

    /**
     * An optional, human-readable description of what the rule does.
     */
    private String description;


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
}
