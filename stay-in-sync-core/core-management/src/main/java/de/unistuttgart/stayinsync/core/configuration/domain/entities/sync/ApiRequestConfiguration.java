package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.List;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "sync_system_type", discriminatorType = DiscriminatorType.STRING)
public abstract class ApiRequestConfiguration extends PanacheEntity {

    public boolean active;

    @OneToOne
    public SyncSystemEndpoint sourceSystemEndpoint;

    @OneToMany(mappedBy = "apiRequestConfiguration")
    public Set<ApiRequestQueryParam> apiQueryParams;

    @OneToMany(mappedBy = "apiRequestConfiguration")
    public Set<ApiRequestHeader> apiRequestHeaders;

    public static List<ApiRequestConfiguration> findByEndpointId(Long endpointId) {
        return find("sourceSystemEndpoint.id", endpointId).list();
    }
}
