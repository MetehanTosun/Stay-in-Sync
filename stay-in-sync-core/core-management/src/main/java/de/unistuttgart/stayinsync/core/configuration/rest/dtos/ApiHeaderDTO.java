package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.transport.domain.ApiRequestHeaderType;

import java.util.Set;

public record ApiHeaderDTO(Long id, ApiRequestHeaderType headerType, String headerName, Set<String> values) {
}
