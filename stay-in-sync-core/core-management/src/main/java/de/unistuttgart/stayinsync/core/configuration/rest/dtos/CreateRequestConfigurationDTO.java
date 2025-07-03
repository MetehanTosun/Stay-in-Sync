package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotNull;

public record CreateRequestConfigurationDTO(@NotNull String name, boolean active, int pollingIntervallTimeInMs) {
}
