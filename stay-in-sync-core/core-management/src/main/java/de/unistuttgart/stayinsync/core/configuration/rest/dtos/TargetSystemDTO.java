package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import java.util.Set;

public record TargetSystemDTO(
        Long id,
        Set<Long> targetSystemEndpointIds) {
}