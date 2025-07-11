package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import java.util.Set;

public record TargetSystemDTO(
                Long id,
                String name,
                String apiUrl,
                String description,
                String apiType,
                String openAPI,
                Set<Long> targetSystemEndpointIds) {
}