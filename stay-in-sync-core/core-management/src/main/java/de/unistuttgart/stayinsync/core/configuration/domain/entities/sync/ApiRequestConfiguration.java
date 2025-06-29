package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "sync_system_type", discriminatorType = DiscriminatorType.STRING)
public abstract class ApiRequestConfiguration extends PanacheEntity {

    public boolean active;

    @ManyToOne
    public SyncSystemEndpoint syncSystemEndpoint;

    @OneToMany(mappedBy = "requestConfiguration")
    public Set<ApiRequestConfigurationQueryParam> queryParameterValues;

    @OneToMany(mappedBy = "requestConfiguration")
    public Set<ApiRequestConfigurationHeader> apiRequestHeaders;
}
