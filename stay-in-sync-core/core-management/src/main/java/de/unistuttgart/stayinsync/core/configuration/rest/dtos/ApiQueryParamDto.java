package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO für einen Query-Parameter des Endpoints.
 */
public record ApiQueryParamDto(
    @Schema(description = "Name des Query-Parameters")
    String name,
    @Schema(description = "Wert des Query-Parameters")
    String value
) {}