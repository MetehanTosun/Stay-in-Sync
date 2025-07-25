package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record SourceSystemEndpointDTO(Long id, Long sourceSystemId, @NotNull String endpointPath,
                                      @NotNull String httpRequestType,
                                      @Schema(description = "Request-Body-Schema als JSON-Schema oder freier String")
                                      String requestBodySchema) {
}
