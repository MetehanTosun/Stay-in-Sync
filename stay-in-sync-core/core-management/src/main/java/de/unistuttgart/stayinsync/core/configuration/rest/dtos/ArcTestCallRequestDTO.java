package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import java.util.Map;

public record ArcTestCallRequestDTO(
        Long sourceSystemId,
        Long endpointId,
        Map<String, String> pathParameters,
        Map<Long, String> queryParameterValues,
        Map<Long, String> headerValues
) {
}
