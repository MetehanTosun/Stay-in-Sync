package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("TARGET_SYSTEM")
public class TargetSystem extends SyncSystem {

}
