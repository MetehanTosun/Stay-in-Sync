package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.transport.domain.ApiAuthType;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "BasicAuthDTO", description = "BASIC auth payload")
public record BasicAuthDTO(
    @Schema(required = true) @NotNull String username,
    @Schema(required = true) @NotNull String password
) implements ApiAuthConfigurationDTO {
    @Override
    public ApiAuthType getAuthType() {
        return ApiAuthType.BASIC;
    }
}
