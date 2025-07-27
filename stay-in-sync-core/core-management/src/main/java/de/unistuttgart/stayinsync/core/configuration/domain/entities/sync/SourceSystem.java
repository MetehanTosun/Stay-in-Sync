package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.Set;

@Entity
@DiscriminatorValue("SOURCE_SYSTEM")
public class SourceSystem extends SyncSystem {

    @OneToMany(mappedBy = "sourceSystem", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<SourceSystemEndpoint> sourceSystemEndpoints;

    @OneToMany(mappedBy = "sourceSystem", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<SourceSystemApiRequestConfiguration> sourceSystemApiRequestConfigurations;
}