package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotNull;

public record SourceSystemDto(Long id, @NotNull String name, @NotNull String apiUrl, String description,
                              @NotNull String apiType,
                              byte[] openApiSpec) {
}


