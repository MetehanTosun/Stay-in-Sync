package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("TARGET_SYSTEM")
public class TargetSystem extends SyncSystem {

}
