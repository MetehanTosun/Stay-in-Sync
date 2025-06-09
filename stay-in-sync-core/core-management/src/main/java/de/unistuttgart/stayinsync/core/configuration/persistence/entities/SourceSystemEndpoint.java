package de.unistuttgart.stayinsync.core.configuration.persistence.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.Set;

@Entity
public class SourceSystemEndpoint extends PanacheEntity {

    public String endpointPath;

    public String httpRequestType;

    @OneToMany(mappedBy = "sourceSystemEndpoint")
    public Set<SourceSystemApiQueryParam> apiQueryParams;

    @OneToMany(mappedBy = "sourceSystemEndpoint")
    public Set<SourceSystemApiRequestHeader> apiRequestHeaders;

    public String jsonSchema;

    public int pollingRateInMs;

    @ManyToOne
    public SourceSystem sourceSystem;

    @OneToMany(mappedBy = "sourceSystemEndpoint")
    public Set<SourceSystemVariable> sourceSystemVariable;
}
