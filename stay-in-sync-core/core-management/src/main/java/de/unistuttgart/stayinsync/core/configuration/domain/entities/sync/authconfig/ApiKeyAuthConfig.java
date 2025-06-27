package de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.authconfig;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("API_KEY")
public class ApiKeyAuthConfig extends SyncSystemAuthConfig {

    public String apiKey;
    public String headerName;
}
