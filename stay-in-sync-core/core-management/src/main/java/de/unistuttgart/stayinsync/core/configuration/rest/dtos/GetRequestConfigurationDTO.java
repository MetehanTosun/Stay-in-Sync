package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.core.transport.dto.ApiRequestParameterMessageDTO;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record GetRequestConfigurationDTO(@NotNull String alias, boolean active, int pollingIntervallTimeInMs,
                                         Set<ApiHeaderDTO> apiRequestHeaders,
                                         Set<ApiRequestParameterMessageDTO> apiRequestParameters,
                                         String responseDts,
                                         String sourceSystemName
) {
}
