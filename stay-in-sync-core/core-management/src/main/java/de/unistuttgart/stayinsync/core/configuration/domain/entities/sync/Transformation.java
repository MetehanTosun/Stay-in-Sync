package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

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

    @OneToOne
    public TransformationRule transformationRule;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "transformation_sourceApiRequestConfiguration",
            joinColumns = @JoinColumn(name = "transformation_id"),
            inverseJoinColumns = @JoinColumn(name = "source_system_api_request_configuration_id")
    )
    public Set<SourceSystemApiRequestConfiguration> sourceSystemApiRequestConfigrations = new HashSet<>();


    @ManyToMany
    @JoinTable(
            name = "transformation_sourceVariable",
            joinColumns = @JoinColumn(name = "transformation_id"),
            inverseJoinColumns = @JoinColumn(name = "source_variable_id")
    )
    public Set<SourceSystemVariable> sourceSystemVariables = new HashSet<>();

    @ManyToOne
    public TargetSystemEndpoint targetSystemEndpoint;
}
