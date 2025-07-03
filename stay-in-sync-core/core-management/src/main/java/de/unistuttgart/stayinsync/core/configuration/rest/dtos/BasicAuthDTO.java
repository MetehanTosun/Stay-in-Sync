package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.transport.domain.ApiAuthType;
import jakarta.validation.constraints.NotNull;

public record BasicAuthDTO(@NotNull String username, @NotNull String password) implements ApiAuthConfigurationDTO {
    @Override
    public ApiAuthType getAuthType() {
        return ApiAuthType.BASIC;
    }
}
