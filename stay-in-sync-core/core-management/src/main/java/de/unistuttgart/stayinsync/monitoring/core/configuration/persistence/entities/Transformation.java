package de.unistuttgart.stayinsync.monitoring.core.configuration.persistence.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

//TODO Implementierung
@Entity
public class Transformation extends PanacheEntity {

    @ManyToOne
    public SyncJob syncJob;

    @OneToOne
    public TransformationScript transformationScript;

    @ManyToMany
    @JoinTable(
            name = "transformation_sourceVariable",
            joinColumns = @JoinColumn(name = "transformation_id"),
            inverseJoinColumns = @JoinColumn(name = "source_variable_id")
    )
    public Set<SourceSystemVariable> sourceSystemVariables = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "transformation_inputVariable",
            joinColumns = @JoinColumn(name = "transformation_id"),
            inverseJoinColumns = @JoinColumn(name = "input_variable_id")
    )
    public Set<OutputSystemVariable> outputSystemVariables = new HashSet<>();
    ;


}
