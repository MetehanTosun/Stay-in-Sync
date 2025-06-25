package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO für eine Pfad-Variable (Path-Parameter) des Endpoints.
 */
public record SourceSystemVariableDto(
    @Schema(description = "Name der Pfad-Variable, z.B. 'userId'")
    String name,
    @Schema(description = "Default- oder Beispielwert der Variable")
    String defaultValue
) {}