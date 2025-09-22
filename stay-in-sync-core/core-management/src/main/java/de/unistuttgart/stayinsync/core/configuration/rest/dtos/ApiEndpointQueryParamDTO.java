package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.core.transport.domain.ApiEndpointQueryParamType;
import de.unistuttgart.stayinsync.core.transport.dto.SchemaType;

import java.util.Set;

public record ApiEndpointQueryParamDTO(
        String paramName,
        ApiEndpointQueryParamType queryParamType,
        SchemaType schemaType,
        Long id,
        Set<String> values
) {
}
