package de.unistuttgart.stayinsync.core.configuration.edc.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public class EDCAccessPolicyPermissionDto {
    private UUID id;

    @NotBlank
    private String action;

    @NotBlank
    private String constraintLeftOperand;

    @NotBlank
    private String constraintOperator;

    @NotBlank
    private String constraintRightOperand;

    public UUID getId() {
        return id;
    }
    public EDCAccessPolicyPermissionDto setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getAction() {
        return action;
    }
    public EDCAccessPolicyPermissionDto setAction(String action) {
        this.action = action;
        return this;
    }

    public String getConstraintLeftOperand() {
        return constraintLeftOperand;
    }
    public EDCAccessPolicyPermissionDto setConstraintLeftOperand(String constraintLeftOperand) {
        this.constraintLeftOperand = constraintLeftOperand;
        return this;
    }

    public String getConstraintOperator() {
        return constraintOperator;
    }
    public EDCAccessPolicyPermissionDto setConstraintOperator(String constraintOperator) {
        this.constraintOperator = constraintOperator;
        return this;
    }

    public String getConstraintRightOperand() {
        return constraintRightOperand;
    }
    public EDCAccessPolicyPermissionDto setConstraintRightOperand(String constraintRightOperand) {
        this.constraintRightOperand = constraintRightOperand;
        return this;
    }
}
