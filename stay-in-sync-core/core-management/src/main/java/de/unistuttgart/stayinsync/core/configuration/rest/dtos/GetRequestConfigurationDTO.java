package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.transport.dto.ApiRequestParameterMessageDTO;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record GetRequestConfigurationDTO(@NotNull String name, boolean used, int pollingIntervallTimeInMs,
                                         Set<ApiHeaderDTO> apiRequestHeaders,
                                         Set<ApiRequestParameterMessageDTO> apiRequestParameters) {
}
