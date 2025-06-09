package de.unistuttgart.stayinsync.core.configuration.persistence.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.util.Set;

@Entity
public class TargetSystem extends PanacheEntity {

    public String name;

    public String description;

    public String apiURL;

    @OneToOne
    TargetSystemAuthDetails authDetails;

    @OneToMany(mappedBy = "targetSystem")
    public Set<Transformation> transformations;

}
