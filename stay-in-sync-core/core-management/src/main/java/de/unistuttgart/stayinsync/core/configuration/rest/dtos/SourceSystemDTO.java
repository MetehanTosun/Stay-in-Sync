package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.transport.domain.ApiAuthType;
import jakarta.validation.constraints.NotNull;

public record SourceSystemDTO(Long id, @NotNull String name, @NotNull String apiUrl, String description,
                              @NotNull String apiType,
                              ApiAuthType apiAuthType,
                              byte[] openApiSpec) {
}


