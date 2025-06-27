package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.edc.EDCAsset;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "sync_system_type", discriminatorType = DiscriminatorType.STRING)
public abstract class SyncSystemEndpoint extends PanacheEntity {

    public String endpointPath;

    public String jsonSchema;

    public String description;

    public String httpRequestType;

    @OneToMany(mappedBy = "syncSystemEndpoint")
    public Set<SyncSystemApiQueryParam> apiQueryParams;

    @OneToMany(mappedBy = "syncSystemEndpoint")
    public Set<SyncSystemApiRequestHeader> apiRequestHeaders;

    @ManyToOne
    public SyncSystem syncSystem;

    @OneToOne
    public EDCAsset asset;
}
