package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotNull;

public record CreateSourceSystemEndpointDTO(@NotNull String endpointPath,
                                            @NotNull String httpRequestType) {
}
