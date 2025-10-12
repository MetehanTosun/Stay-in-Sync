package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.authconfig;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.SyncSystem;
import de.unistuttgart.stayinsync.transport.domain.ApiAuthType;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

/**
 * Superclass that serves the purpose of describing auth details of systems that are involved in a sync process
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "auth_type", discriminatorType = DiscriminatorType.STRING)
public abstract class SyncSystemAuthConfig extends PanacheEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "api_auth_type")
    public ApiAuthType authType;

    @OneToOne
    public SyncSystem syncSystem;
}
