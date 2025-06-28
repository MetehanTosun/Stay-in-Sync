
package de.unistuttgart.stayinsync.core.configuration.rest.dtos;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for a source system configuration.
 */
@Schema(name = "SourceSystem", description = "DTO for a source system configuration")
public record SourceSystemDto(
    @Schema(description = "Unique identifier", example = "42")
    @NotNull
    Long id,

    @Schema(description = "Name of the source system", example = "System A")
    @NotBlank
    String name,

    @Schema(description = "Description of the source system", example = "This system handles user data")
    String description,

    @Schema(description = "Type of the source system", implementation = SourceSystemType.class)
    @NotNull
    SourceSystemType type,

    @Schema(description = "API URL or AAS endpoint", example = "https://api.systema.com")
    @NotBlank
    String apiUrl,

    @Schema(description = "Authentication type for the source system", implementation = AuthType.class)
    AuthType authType,

    @Schema(description = "Username for BASIC auth", example = "user1")
    String username,

    @Schema(description = "Password for BASIC auth", example = "secret")
    String password,

    @Schema(description = "API key for API_KEY auth", example = "xyz-9876")
    String apiKey,

    @Schema(description = "URL to the OpenAPI specification", example = "https://api.systema.com/openapi.json")
    String openApiSpecUrl,

    @Schema(description = "Raw OpenAPI specification content")
    String openApiSpec
) {}