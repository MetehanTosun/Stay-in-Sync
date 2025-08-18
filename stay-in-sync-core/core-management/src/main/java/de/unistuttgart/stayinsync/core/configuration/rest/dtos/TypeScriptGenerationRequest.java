package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import jakarta.validation.constraints.NotBlank;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Request for TypeScript generation from JSON schema")
public class TypeScriptGenerationRequest {
    
    @NotBlank(message = "JSON schema is required")
    @Schema(description = "JSON schema string to generate TypeScript interface from", required = true)
    private String jsonSchema;

    public TypeScriptGenerationRequest() {}

    public TypeScriptGenerationRequest(String jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    public String jsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(String jsonSchema) {
        this.jsonSchema = jsonSchema;
    }
} 