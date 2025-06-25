package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.authconfig.SyncSystemAuthConfig;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

/**
 * Superclass that serves the purpose of describing Target and Source system involved in a sync process
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "sync_system_type", discriminatorType = DiscriminatorType.STRING)
public abstract class SyncSystem extends PanacheEntity {

    public String name;
    public String apiUrl;
    public String description;
    public String apiType; // REST, AAS
    public String openAPI;

    @OneToOne
    SyncSystemAuthConfig systemAuthConfig;
}
