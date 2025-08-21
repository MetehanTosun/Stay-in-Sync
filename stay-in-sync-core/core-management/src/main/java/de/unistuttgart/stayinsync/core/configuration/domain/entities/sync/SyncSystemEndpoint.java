package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "sync_system_type", discriminatorType = DiscriminatorType.STRING)
public abstract class SyncSystemEndpoint extends PanacheEntity {

    public String endpointPath;

    public String jsonSchema;

    @Lob
    public String requestBodySchema;

    @Lob
    public String responseBodySchema;

    @Lob
    public String responseDts;

    public String description;

    public String httpRequestType;

    @OneToMany(mappedBy = "syncSystemEndpoint")
    public Set<ApiEndpointQueryParam> queryParams;

    @OneToMany(mappedBy = "syncSystemEndpoint")
    public Set<ApiRequestConfiguration> apiRequestConfiguration;

    @ManyToOne
    @JoinColumn(name = "sync_system_id")
    public SyncSystem syncSystem;
}
