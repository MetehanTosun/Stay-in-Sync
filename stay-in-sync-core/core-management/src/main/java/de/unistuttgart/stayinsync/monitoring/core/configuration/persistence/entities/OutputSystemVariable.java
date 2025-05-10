package de.unistuttgart.stayinsync.monitoring.core.configuration.persistence.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.Set;

//TODO Implementierung
@Entity
public class OutputSystemVariable extends PanacheEntity {

    public String name;

    @ManyToMany(mappedBy = "outputSystemVariables")
    public Set<Transformation> transformations = new HashSet<>();
}
