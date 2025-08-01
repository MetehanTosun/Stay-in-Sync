package de.unistuttgart.stayinsync.core.configuration.edc;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class EDCAccessPolicyPermission extends PanacheEntity {

    @ManyToOne
    public EDCAccessPolicy edcAccessPolicy;

    public String action;

    public String constraintLeftOperand;

    public String constraintOperator;
    
    public String constraintRightOperand;

    // --- Getter & Setter ---
    public EDCAccessPolicy getEdcAccessPolicy() {
        return edcAccessPolicy;
    }

    public void setEdcAccessPolicy(EDCAccessPolicy edcAccessPolicy) {
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
}
