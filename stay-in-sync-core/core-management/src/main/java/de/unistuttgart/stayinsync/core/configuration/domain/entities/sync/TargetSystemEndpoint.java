package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.edc.EDCAsset;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

@Entity
public class TargetSystemEndpoint extends SyncSystemEndpoint {

    @OneToOne
    public EDCAsset asset;
}
