package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

public record SourceSystemEndpointDTO(Long id, Long sourceSystemId, String endpointPath, String httpRequestType) {
}
