package de.unistuttgart.stayinsync.core.configuration.persistence.entities.edc;

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

}
