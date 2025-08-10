package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class EDCPropertyDto {
    private UUID id;

    @NotBlank
    private String description;

    public UUID getId() {
        return id;
    }

    public EDCPropertyDto setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public EDCPropertyDto setDescription(String description) {
        this.description = description;
        return this;
    }
}
