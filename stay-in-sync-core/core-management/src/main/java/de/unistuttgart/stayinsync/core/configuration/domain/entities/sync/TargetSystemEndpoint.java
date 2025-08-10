package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.util.Set;

import de.unistuttgart.stayinsync.core.configuration.edc.EDCAsset;

@Entity
public class TargetSystemEndpoint extends SyncSystemEndpoint {

    public String endpointPath;

    public String jsonSchema;

    public String description;

    public String httpRequestType;

    @OneToMany(mappedBy = "targetSystemEndpoint")
    public Set<TargetSystemApiQueryParam> apiQueryParams;

    @OneToMany(mappedBy = "targetSystemEndpoint")
    public Set<TargetSystemApiRequestHeader> apiRequestHeaders;

    @OneToMany(mappedBy = "targetSystemEndpoint")
    public Set<TargetSystemVariable> targetSystemVariable;

    @ManyToOne
    public TargetSystem targetSystem;

    @OneToOne
    public EDCAsset asset;
}
