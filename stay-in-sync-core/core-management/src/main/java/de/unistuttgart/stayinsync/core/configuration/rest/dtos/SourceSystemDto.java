package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "SourceSystem", description = "DTO for a source system configuration")
public record SourceSystemDto(
    @Schema(description = "Unique identifier", example = "42")
    @NotNull
    Long id,

    @Schema(description = "Name of the source system", example = "System A")
    @NotBlank
    String name,

    @Schema(description = "Description of the source system", example = "This system handles user data")
    @NotNull
    String description,

    @Schema(description = "Type of the source system", example = "REST API")
    @NotBlank
    String type,

    @Schema(description = "API URL of the source system", example = "https://api.systema.com")
    @NotBlank
    String apiUrl,

    @Schema(description = "URL to the OpenAPI specification", example = "https://api.systema.com/openapi.json")
    String openApiSpecUrl,

    @Schema(description = "OpenAPI specification content")
    String openApiSpec
) {}
