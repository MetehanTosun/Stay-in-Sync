package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

//TODO Implementierung
@Entity
public class TransformationScript extends PanacheEntity {

    public String name;

    public String scriptCode;

    public String hash;

    @OneToOne
    public Transformation transformation;

}
