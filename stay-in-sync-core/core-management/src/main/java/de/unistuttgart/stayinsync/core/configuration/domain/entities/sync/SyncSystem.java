package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.authconfig.SyncSystemAuthConfig;
import de.unistuttgart.stayinsync.transport.domain.ApiAuthType;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "api_auth_type")
    public ApiAuthType apiAuthType;
    @Lob
    public byte[] openApiSpec;

    @OneToMany(mappedBy = "syncSystem")
    Set<SyncSystemEndpoint> syncSystemEndpoints;

    @OneToMany(mappedBy = "syncSystem")
    Set<ApiHeader> apiRequestHeaders;

    @OneToOne
    SyncSystemAuthConfig systemAuthConfig;
}
