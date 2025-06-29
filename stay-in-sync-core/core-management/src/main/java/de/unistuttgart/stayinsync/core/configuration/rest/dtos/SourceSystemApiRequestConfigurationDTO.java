package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.transport.dto.ApiRequestParameterMessageDTO;

import java.util.Set;

public record SourceSystemApiRequestConfigurationDTO(Long id, boolean active, int pollingIntervallTimeInMs,
                                                     SourceSystemDTO sourceSystem,
                                                     SourceSystemEndpointDTO endpoint,
                                                     Set<ApiRequestHeaderDTO> apiRequestHeaders,
                                                     Set<ApiRequestParameterMessageDTO> apiRequestParameters) {
}
