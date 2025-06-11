package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

//TODO Implementierung
@Entity
public class TargetSystemVariable extends PanacheEntity {

    public String name;

    public String targetSystemVariableId;

    @ManyToOne
    public Transformation transformation;

    @ManyToOne
    public TargetSystemEndpoint targetSystemEndpoint;
}
