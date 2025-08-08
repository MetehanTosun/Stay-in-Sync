package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TargetSystemEndpointDTO(Long id,
                                      Long targetSystemId,
                                      @NotBlank String endpointPath,
                                      @NotBlank @Pattern(regexp = "GET|POST|PUT|DELETE|PATCH") String httpRequestType,
                                      String description,
                                      String jsonSchema) {
}


