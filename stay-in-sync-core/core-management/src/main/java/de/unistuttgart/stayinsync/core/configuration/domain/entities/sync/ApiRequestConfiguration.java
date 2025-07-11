package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.List;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "sync_system_type", discriminatorType = DiscriminatorType.STRING)
public abstract class ApiRequestConfiguration extends PanacheEntity {

    public String name;

    public boolean active;

    @ManyToOne
    public SyncSystemEndpoint syncSystemEndpoint;

    @OneToMany(mappedBy = "requestConfiguration")
    public Set<ApiEndpointQueryParamValue> queryParameterValues;

    @OneToMany(mappedBy = "requestConfiguration")
    public Set<ApiHeaderValue> apiRequestHeaders;

    public List<ApiHeaderValue> findRequestHeadersByConfigurationId(Long requestConfigurationId) {
        return ApiHeaderValue.list("requestConfiguration.id", requestConfigurationId);
    }


}
