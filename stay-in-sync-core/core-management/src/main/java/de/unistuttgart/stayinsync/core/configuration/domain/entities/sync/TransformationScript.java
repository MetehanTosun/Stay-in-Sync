package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;

//TODO Implementierung
@Entity
public class TransformationScript extends PanacheEntity {

    public String name;

    @Lob
    public String typescriptCode;

    @Lob
    public String javascriptCode;

    public String hash;

    @OneToOne
    public Transformation transformation;

}
