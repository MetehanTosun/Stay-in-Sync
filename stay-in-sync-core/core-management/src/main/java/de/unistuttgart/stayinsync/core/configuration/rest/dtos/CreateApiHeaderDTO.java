package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.transport.domain.ApiRequestHeaderType;

public record CreateApiHeaderDTO(ApiRequestHeaderType headerType, String headerName) {
}
