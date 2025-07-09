package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotNull;

public record SyncJobDTO(
        Long id,
        @NotNull String name,
        boolean deployed
) {
}
