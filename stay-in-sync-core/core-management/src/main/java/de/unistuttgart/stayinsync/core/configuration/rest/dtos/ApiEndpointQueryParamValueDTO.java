package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

public record ApiEndpointQueryParamValueDTO(Long id, Long queryParamId, String paramName, String paramValue) {
}
