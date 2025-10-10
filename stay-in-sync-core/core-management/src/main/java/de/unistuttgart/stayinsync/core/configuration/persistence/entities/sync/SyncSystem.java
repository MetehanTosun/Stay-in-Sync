package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync;

import de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.authconfig.SyncSystemAuthConfig;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Superclass that serves the purpose of describing Target and Source system involved in a sync process
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "sync_system_type", discriminatorType = DiscriminatorType.STRING)
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_syncsystem_apiType_apiUrl_aasId", columnNames = {"apiType", "apiUrl", "aasId"})
})
public abstract class SyncSystem extends PanacheEntity {

    public String name;

    public String apiUrl;

    @Lob
    public String description;

    public String apiType;

    public String aasId;

    @Lob
    public String openApiSpec;

    @OneToMany(mappedBy = "syncSystem")
    public Set<SyncSystemEndpoint> syncSystemEndpoints = new HashSet<>();

    // Removed the @OneToMany relationship to avoid inheritance issues during deletion
    // ApiHeaders are now managed manually in the service layer

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public SyncSystemAuthConfig authConfig;
}
