package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.Set;

@Entity
@DiscriminatorValue("SOURCE_SYSTEM")
public class SourceSystem extends SyncSystem {

    @OneToMany(mappedBy = "sourceSystem")
    public Set<SourceSystemEndpoint> sourceSystemEndpoints;

}