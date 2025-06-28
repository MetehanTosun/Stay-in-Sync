package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CreateSourceSystem", description = "Payload for creating a source system")
public record CreateSourceSystemJsonDTO(
    @Schema(description = "Name", example = "Petstore")
    
    String name,

    @Schema(description = "Description", example = "Swagger Petstore")
    String description,

    @Schema(description = "Type", example = "REST_OPENAPI")
    @NotNull
    SourceSystemType type,

    @Schema(description = "API base URL", example = "https://petstore3.swagger.io")
    @NotBlank
    String apiUrl,

    @Schema(description = "Auth type", example = "API_KEY")
    AuthType authType,

    @Schema(description = "Username for BASIC auth", example = "user1")
    String username,

    @Schema(description = "Password for BASIC auth", example = "secret")
    String password,

    @Schema(description = "API key for API_KEY auth", example = "demo-key")
    String apiKey,

    @Schema(description = "Link to OpenAPI spec", example = "https://…/openapi.json")
    String openApiSpecUrl,

    @Schema(description = "Raw OpenAPI spec JSON/YAML")
    String openApiSpec
) {}