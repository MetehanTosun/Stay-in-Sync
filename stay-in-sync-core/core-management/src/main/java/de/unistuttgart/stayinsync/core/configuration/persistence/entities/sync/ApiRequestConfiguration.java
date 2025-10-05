package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "sync_system_type", discriminatorType = DiscriminatorType.STRING)
public abstract class ApiRequestConfiguration extends PanacheEntity {

    public String alias;

    public boolean active;

    @ManyToOne
    public SyncSystemEndpoint syncSystemEndpoint;

    @OneToMany(mappedBy = "requestConfiguration")
    public Set<ApiEndpointQueryParamValue> queryParameterValues = new HashSet<>();

    @OneToMany(mappedBy = "requestConfiguration")
    public Set<ApiHeaderValue> apiRequestHeaders = new HashSet<>();

    public List<ApiHeaderValue> findRequestHeadersByConfigurationId(Long requestConfigurationId) {
        return list("requestConfiguration.id", requestConfigurationId);
    }


}
