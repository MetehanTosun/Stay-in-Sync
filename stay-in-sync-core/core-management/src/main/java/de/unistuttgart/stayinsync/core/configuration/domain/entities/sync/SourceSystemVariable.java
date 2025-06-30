package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

import java.util.HashSet;
import java.util.Set;

//TODO Implementierung
@Entity
public class SourceSystemVariable extends PanacheEntity {

    public String name;

    public String jsonObjectKey;

    @ManyToOne
    public SourceSystemEndpoint sourceSystemEndpoint; // /description

    @ManyToOne
    public SourceSystem sourceSystem;

    @ManyToMany(mappedBy = "sourceSystemVariables")
    public Set<Transformation> transformations = new HashSet<>();


}
