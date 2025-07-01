package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.authconfig.SyncSystemAuthConfig;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.Set;

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

    @Lob
    public byte[] openApiSpec;

    @OneToMany(mappedBy = "syncSystem")
    public Set<SyncSystemEndpoint> syncSystemEndpoints;

    @OneToMany(mappedBy = "syncSystem")
    public Set<ApiHeader> apiRequestHeaders;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public SyncSystemAuthConfig authConfig;
}
