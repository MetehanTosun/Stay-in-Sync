package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.transport.domain.ApiAuthType;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "API-Key auth payload (apiKey + headerName)")
public record ApiKeyAuthDTO(
    @NotNull @Schema(required = true) String apiKey,
    @NotNull @Schema(required = true) String headerName
) implements ApiAuthConfigurationDTO {
    @Override
    public ApiAuthType getAuthType() {
        return ApiAuthType.API_KEY;
    }
}
