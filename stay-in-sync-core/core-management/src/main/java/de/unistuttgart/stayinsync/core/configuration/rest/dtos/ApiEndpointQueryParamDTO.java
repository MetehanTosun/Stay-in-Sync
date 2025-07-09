package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.transport.domain.ApiEndpointQueryParamType;

import java.util.Set;

public record ApiEndpointQueryParamDTO(String paramName, ApiEndpointQueryParamType queryParamType, Long id,
                                       Set<String> values
) {
}
