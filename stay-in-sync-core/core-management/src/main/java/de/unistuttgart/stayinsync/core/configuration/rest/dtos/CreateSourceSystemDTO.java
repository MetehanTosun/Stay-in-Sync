package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.transport.domain.ApiAuthType;
import de.unistuttgart.stayinsync.transport.dto.ApiAuthConfigurationMessageDTO;
import jakarta.validation.constraints.NotNull;

public record CreateSourceSystemDTO(Long id, @NotNull String name, @NotNull String apiUrl, String description,
                                    @NotNull String apiType,
                                    ApiAuthType apiAuthType,
                                    ApiAuthConfigurationMessageDTO authConfig,
                                    byte[] openApiSpec) {
}