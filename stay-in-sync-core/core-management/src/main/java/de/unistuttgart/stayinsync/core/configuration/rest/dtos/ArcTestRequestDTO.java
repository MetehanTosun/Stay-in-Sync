package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import java.util.Map;

public record ArcTestRequestDTO(
        Long sourceSystemId,
        Long endpointId,
        Map<String, String> pathParameters,
        Map<String, String> queryParameterValues,
        Map<String, String> headerValues
) {
}
