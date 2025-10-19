package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "edc_access_policy_permission")
@NoArgsConstructor
public class AccessPolicyPermission extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "policy_id", nullable = false)
    public Policy edcAccessPolicy;

    @Column(nullable = false)
    public String action;

    @Column(name = "constraint_left_operand", nullable = false)
    public String constraintLeftOperand;

    @Column(name = "constraint_operator", nullable = false)
    public String constraintOperator;
    
    @Column(name = "constraint_right_operand", nullable = false)
    public String constraintRightOperand;

    public Policy getEdcAccessPolicy() {
        return edcAccessPolicy;
    }

    public void setEdcAccessPolicy(Policy edcAccessPolicy) {
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
