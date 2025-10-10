package de.unistuttgart.stayinsync.core.configuration.persistence.entities.aas;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SourceSystem;
import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.Transformation;
import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Optional;
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

    /**
     * Finds an AAS ARC by its unique combination of Source System name and ARC alias.
     */
    public static Optional<AasSourceApiRequestConfiguration> findBySourceSystemAndArcName(String sourceSystemName, String arcName) {
        return find("sourceSystem.name = ?1 and alias = ?2", sourceSystemName, arcName).firstResultOptional();
    }
}
