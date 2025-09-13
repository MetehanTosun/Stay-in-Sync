package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@DiscriminatorValue("SOURCE_SYSTEM")
@Table(
    uniqueConstraints = @UniqueConstraint(columnNames = {"sync_system_id", "endpointPath", "httpRequestType"})
)
public class SourceSystemEndpoint extends SyncSystemEndpoint {

    @ManyToOne
    @JoinColumn(name = "sync_system_id", insertable = false, updatable = false)
    public SourceSystem sourceSystem;

    @OneToMany(mappedBy = "sourceSystemEndpoint")
    public Set<SourceSystemApiRequestConfiguration> apiRequestConfigurations = new HashSet<>();

    @OneToMany(mappedBy = "sourceSystemEndpoint")
    public Set<SourceSystemVariable> sourceSystemVariable = new HashSet<>();

    public static List<SourceSystemEndpoint> findBySourceSystemId(Long sourceSystemId) {
        return find("sourceSystem.id", sourceSystemId).list();
    }

    public static List<SourceSystemApiRequestConfiguration> findByEndpointId(Long endpointId) {
        return find("sourceSystemEndpoint.id", endpointId).list();
    }
}