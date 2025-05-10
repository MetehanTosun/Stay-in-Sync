package de.unistuttgart.stayinsync.monitoring.core.configuration.persistence.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

//TODO Implementierung
@Entity
public class TransformationScript extends PanacheEntity {

    public String name;

    String scriptCode;

    String hash;

    @OneToOne
    Transformation transformation;

}
