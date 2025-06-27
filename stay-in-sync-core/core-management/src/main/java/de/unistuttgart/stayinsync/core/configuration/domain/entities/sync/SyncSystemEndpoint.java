package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "sync_system_type", discriminatorType = DiscriminatorType.STRING)
public abstract class SyncSystemEndpoint extends PanacheEntity {

    public String endpointPath;

    public String jsonSchema;

    public String description;

    public String httpRequestType;

    @ManyToOne
    public SyncSystem syncSystem;
}
