package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record TargetSystemEndpointDTO(Long id,
                                      Long targetSystemId,
                                      @NotBlank String endpointPath,
                                      @NotBlank @Pattern(regexp = "GET|POST|PUT|DELETE|PATCH") String httpRequestType,
                                      @Schema(description = "Request-Body-Schema as JSON schema or free-form string")
                                      String requestBodySchema,
                                      @Schema(description = "Response-Body-Schema as JSON schema or free-form string")
                                      String responseBodySchema,
                                      @Schema(description = "Generated TypeScript types from response body schema")
                                      String responseDts,
                                      String description,
                                      String jsonSchema) {
}


