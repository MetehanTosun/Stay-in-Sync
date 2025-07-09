package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.transport.domain.ApiAuthType;

public record ApiKeyAuthDTO(String apiKey, String headerName) implements ApiAuthConfigurationDTO {
    @Override
    public ApiAuthType getAuthType() {
        return ApiAuthType.API_KEY;
    }
}
