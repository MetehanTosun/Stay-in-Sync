package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO für einen HTTP-Request-Header des Endpoints.
 */
public record ApiRequestHeaderDto(
    @Schema(description = "Name des HTTP-Headers")
    String name,
    @Schema(description = "Wert des HTTP-Headers")
    String value
) {}