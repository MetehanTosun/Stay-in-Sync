package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record SyncJobCreationDTO(
        Long id,
        @NotNull String name,
        boolean deployed,
        String description,
        boolean isSimulation,
        Set<Long> transformationIds
) {
}

