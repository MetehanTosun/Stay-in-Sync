package de.unistuttgart.stayinsync.core.configuration.persistence.entities.sync.authconfig;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("API_KEY")
public class ApiKeyAuthConfig extends SyncSystemAuthConfig {
    public String apiKey;
    public String headerName;
}
