package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;

@Entity
public class TransformationRule extends PanacheEntity {

    public String name;

    @OneToOne
    public Transformation transformation;

    public int updateIntervall;

    @Lob
    public String graphJsonString;
}
