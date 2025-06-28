// src/main/java/de/unistuttgart/stayinsync/core/configuration/rest/dtos/SourceSystemEndpointDto.java
package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "SourceSystemEndpoint", description = "DTO for a source system endpoint")
public record SourceSystemEndpointDto(
    @Schema(description = "Unique identifier", example = "123")
    Long id,

    @Schema(description = "Path of the endpoint", example = "/pets")
    String endpointPath,

    @Schema(description = "HTTP method", example = "GET")
    String httpRequestType,

    @Schema(description = "Polling enabled")
    boolean pollingActive,

    @Schema(description = "Polling interval in ms", example = "60000")
    int pollingRateInMs,

    @Schema(description = "JSON Schema content (auto or manual)")
    String jsonSchema,

    @Schema(description = "Schema mode ('auto' or 'manual')", example = "auto")
    String schemaMode
) {}