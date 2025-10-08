package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "edc_access_policy_permission")
public class Policy extends UuidEntity {

    @ManyToOne
    @JoinColumn(name = "policy_id", columnDefinition = "CHAR(36)", nullable = false)
    public PolicyDefinition edcAccessPolicy;

    @Column(nullable = false)
    public String action;

    @Column(name = "constraint_left_operand", nullable = false)
    public String constraintLeftOperand;

    @Column(name = "constraint_operator", nullable = false)
    public String constraintOperator;
    
    @Column(name = "constraint_right_operand", nullable = false)
    public String constraintRightOperand;

    public PolicyDefinition getEdcAccessPolicy() {
        return edcAccessPolicy;
    }

    public void setEdcAccessPolicy(PolicyDefinition edcAccessPolicy) {
        this.edcAccessPolicy = edcAccessPolicy;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getConstraintLeftOperand() {
        return constraintLeftOperand;
    }

    public void setConstraintLeftOperand(String constraintLeftOperand) {
        this.constraintLeftOperand = constraintLeftOperand;
    }

    public String getConstraintOperator() {
        return constraintOperator;
    }

    public void setConstraintOperator(String constraintOperator) {
        this.constraintOperator = constraintOperator;
    }

    public String getConstraintRightOperand() {
        return constraintRightOperand;
    }

    public void setConstraintRightOperand(String constraintRightOperand) {
        this.constraintRightOperand = constraintRightOperand;
    }

    // Getter/Setter…
}
