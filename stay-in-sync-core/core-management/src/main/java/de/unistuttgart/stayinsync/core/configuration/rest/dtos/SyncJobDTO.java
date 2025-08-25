package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record SyncJobDTO(
        Long id,
        @NotNull String name,
        String description,
        boolean isSimulation,
        Set<SyncJobTransformationDTO> transformations
) {
}


