package de.unistuttgart.stayinsync.core.configuration.domain.entities.aas;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.Transformation;
import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class AasSourceApiRequestConfiguration extends PanacheEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sourcesystem_id", nullable = false)
    public SourceSystem sourceSystem;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "submodel_id", nullable = false)
    public AasSubmodelLite submodel;

    @Column(nullable = false)
    public String alias;

    public boolean active = true;

    public int pollingIntervallTimeInMs = 20000;

    @Lob
    public String responseDts;

    @Enumerated(EnumType.STRING)
    public JobDeploymentStatus deploymentStatus = JobDeploymentStatus.UNDEPLOYED;

    @ManyToMany(mappedBy = "aasSourceApiRequestConfigurations")
    public Set<Transformation> transformations = new HashSet<>();
}
