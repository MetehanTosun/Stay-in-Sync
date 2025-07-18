package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.Transformation;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record SyncJobDTO(
        Long id,
        @NotNull String name,
        boolean deployed,
        String description,
        boolean isSimulation,
        Set<Transformation> transformations
) {
}
