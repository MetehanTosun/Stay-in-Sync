package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record CreateSourceSystemEndpointDTO(@NotNull String endpointPath,
                                            @NotNull String httpRequestType,
                                            @Schema(description = "Request-Body-Schema als JSON-Schema oder freier String")
                                            String requestBodySchema,
                                            @Schema(description = "Response-Body-Schema als JSON-Schema oder freier String")
                                            String responseBodySchema) {
}
