package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Authentication type for the source system")
public enum AuthType {
    @Schema(description = "Basic Authentication (username + password)")
    BASIC,

    @Schema(description = "API Key Authentication")
    API_KEY
}