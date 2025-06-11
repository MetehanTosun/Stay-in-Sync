package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import java.util.Set;

@Entity
public class SourceSystem extends SyncSystem {

    @OneToMany(mappedBy = "sourceSystem")
    public Set<SourceSystemEndpoint> sourceSystemEndpoint;

    @OneToOne
    public SourceSystemAuthDetails authDetails;
}