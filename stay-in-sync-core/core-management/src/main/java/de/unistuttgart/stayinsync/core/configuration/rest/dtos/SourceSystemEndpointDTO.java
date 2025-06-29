package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotNull;

public record SourceSystemEndpointDTO(Long id, Long sourceSystemId, @NotNull String endpointPath,
                                      @NotNull String httpRequestType) {
}
