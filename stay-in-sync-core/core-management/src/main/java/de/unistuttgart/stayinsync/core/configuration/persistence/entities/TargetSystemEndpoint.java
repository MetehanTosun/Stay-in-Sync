package de.unistuttgart.stayinsync.core.configuration.persistence.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.util.Set;

@Entity
public class TargetSystemEndpoint extends PanacheEntity {

    public String endpointPath;

    public String jsonSchema;

    public int pollingRateInMs;

    public String httpRequestType;

    @OneToMany(mappedBy = "targetSystemEndpoint")
    public Set<TargetSystemApiQueryParam> apiQueryParams;

    @OneToMany(mappedBy = "targetSystemEndpoint")
    public Set<TargetSystemApiRequestHeader> apiRequestHeaders;

    @OneToMany(mappedBy = "targetSystemEndpoint")
    public Set<TargetSystemVariable> targetSystemVariable;

    @OneToOne
    public EDCAsset asset;
}
