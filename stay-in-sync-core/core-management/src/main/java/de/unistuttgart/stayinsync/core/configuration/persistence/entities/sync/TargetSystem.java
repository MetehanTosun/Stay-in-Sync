package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

@Entity
public class TargetSystem extends SyncSystem {

    @OneToOne
    TargetSystemAuthDetails authDetails;

}
