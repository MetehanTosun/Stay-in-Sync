package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.authconfig;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("BASIC")
public class BasicAuthConfig extends SyncSystemAuthConfig {
    public String username;
    public String password;

}
