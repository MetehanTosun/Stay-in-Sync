package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.authconfig;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SyncSystem;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

/**
 * Superclass that serves the purpose of describing auth details of systems that are involved in a sync process
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "auth_type", discriminatorType = DiscriminatorType.STRING)
public abstract class SyncSystemAuthConfig extends PanacheEntity {

    public String authType;

    @OneToOne
    public SyncSystem syncSystem;
}
