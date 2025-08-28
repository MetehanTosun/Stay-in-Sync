package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotNull;

public record SourceSystemDTO(Long id, @NotNull String name, @NotNull String apiUrl, String description,
                              @NotNull String apiType,
                              String aasId,
                              String openApiSpec) {
}


