package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.util.Set;

@Entity
public class TargetSystem extends SyncSystem {

    @OneToOne
    TargetSystemAuthDetails authDetails;

    @OneToMany(mappedBy = "targetSystem")
    public Set<TargetSystemEndpoint> targetSystemEndpoints;
}
