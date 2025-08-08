package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.edc.EDCAsset;
import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.util.Set;

@Entity
@DiscriminatorValue("TARGET_SYSTEM")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"sync_system_id", "endpointPath"}))
public class TargetSystemEndpoint extends SyncSystemEndpoint {

    @OneToMany(mappedBy = "targetSystemEndpoint")
    public Set<TargetSystemApiQueryParam> apiQueryParams;

    @OneToMany(mappedBy = "targetSystemEndpoint")
    public Set<TargetSystemApiRequestHeader> apiRequestHeaders;

    @OneToMany(mappedBy = "targetSystemEndpoint")
    public Set<TargetSystemVariable> targetSystemVariable;

    @ManyToOne
    @JoinColumn(name = "sync_system_id", insertable = false, updatable = false)
    public TargetSystem targetSystem;

    @OneToOne
    public EDCAsset asset;

    public static java.util.List<TargetSystemEndpoint> findByTargetSystemId(Long targetSystemId) {
        return find("targetSystem.id", targetSystemId).list();
    }
}
