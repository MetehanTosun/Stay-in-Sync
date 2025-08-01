package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import jakarta.validation.constraints.NotBlank;

public class EDCPropertyDto {
    private Long id;
    @NotBlank
    private String description;

    public Long getId() {
        return id;
    }
    public EDCPropertyDto setId(Long id) {
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
