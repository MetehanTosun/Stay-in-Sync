package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

// Update the import to the correct package where EDCAsset is located
//import de.unistuttgart.stayinsync.core.configuration.domain.entities.edc.EDCAsset;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.HashSet;

@Entity
@DiscriminatorValue("TARGET_SYSTEM")
@Table(
    uniqueConstraints = @UniqueConstraint(columnNames = {"sync_system_id", "endpointPath", "httpRequestType"})
)
public class TargetSystemEndpoint extends SyncSystemEndpoint {

    @OneToMany(mappedBy = "targetSystemEndpoint")
    public java.util.Set<TargetSystemApiQueryParam> apiQueryParams = new HashSet<>();

    @OneToMany(mappedBy = "targetSystemEndpoint")
    public java.util.Set<TargetSystemApiRequestHeader> apiRequestHeaders = new HashSet<>();

    @OneToMany(mappedBy = "targetSystemEndpoint")
    public java.util.Set<TargetSystemVariable> targetSystemVariable = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "sync_system_id", insertable = false, updatable = false)
    public TargetSystem targetSystem;
    @OneToOne
    //public EDCAsset asset;

    public static java.util.List<TargetSystemEndpoint> findByTargetSystemId(Long targetSystemId) {
        return find("targetSystem.id", targetSystemId).list();
    }
}
