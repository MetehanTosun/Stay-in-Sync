package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import jakarta.validation.constraints.NotNull;

public class EDCAccessPolicyPermissionDto {
    private Long id;

    @NotNull
    private String action;

    @NotNull
    private String constraintLeftOperand;

    @NotNull
    private String constraintOperator;

    @NotNull
    private String constraintRightOperand;

    public Long getId() {
        return id;
    }
    public EDCAccessPolicyPermissionDto setId(Long id) {
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
