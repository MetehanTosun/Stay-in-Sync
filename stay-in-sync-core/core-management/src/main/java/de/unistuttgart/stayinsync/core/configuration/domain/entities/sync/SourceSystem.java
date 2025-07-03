package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Column;

import java.util.Set;

import de.unistuttgart.stayinsync.core.configuration.rest.dtos.AuthType;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.SourceSystemType;

@Entity
@DiscriminatorValue("SOURCE_SYSTEM")
public class SourceSystem extends SyncSystem {

    @OneToMany(mappedBy = "sourceSystem")
    public Set<SourceSystemEndpoint> sourceSystemEndpoints;

    @OneToMany(mappedBy = "sourceSystem")
    public Set<SourceSystemApiRequestConfiguration> sourceSystemApiRequestConfigurations;
}