package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.Set;

@Entity
@DiscriminatorValue("TARGET_SYSTEM")
public class TargetSystem extends SyncSystem {

    @OneToMany(mappedBy = "targetSystem")
    public Set<TargetSystemEndpoint> targetSystemEndpoints;

}
