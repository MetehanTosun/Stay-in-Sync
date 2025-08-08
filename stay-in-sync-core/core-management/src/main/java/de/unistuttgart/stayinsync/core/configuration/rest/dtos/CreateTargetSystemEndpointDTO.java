package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateTargetSystemEndpointDTO(
        @NotBlank String endpointPath,
        @NotBlank @Pattern(regexp = "GET|POST|PUT|DELETE|PATCH") String httpRequestType
) {
}


