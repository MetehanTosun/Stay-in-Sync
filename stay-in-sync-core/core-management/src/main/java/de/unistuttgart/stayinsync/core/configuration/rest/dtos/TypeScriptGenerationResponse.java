package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Response for TypeScript generation")
public class TypeScriptGenerationResponse {
    
    @Schema(description = "Generated TypeScript interface string", example = "interface ResponseBody { id: number; name: string; }")
    private String generatedTypeScript;
    
    @Schema(description = "Error message if generation failed", example = "Invalid JSON schema provided")
    private String error;

    public TypeScriptGenerationResponse() {}

    public TypeScriptGenerationResponse(String generatedTypeScript, String error) {
        this.generatedTypeScript = generatedTypeScript;
        this.error = error;
    }

    public String getGeneratedTypeScript() {
        return generatedTypeScript;
    }

    public void setGeneratedTypeScript(String generatedTypeScript) {
        this.generatedTypeScript = generatedTypeScript;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
} 