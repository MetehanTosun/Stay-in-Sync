package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

public record SyncJobDTO(
        Long id,
        String name,
        boolean deployed,
        String description,
        boolean isSimulation
) {
}
