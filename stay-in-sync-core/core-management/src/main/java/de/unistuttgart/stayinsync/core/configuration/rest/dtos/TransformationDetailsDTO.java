package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import java.util.Set;

public record TransformationDetailsDTO(
        Long id,
        String name,
        String description,
        Long syncJobId,
        Set<Long> sourceSystemEndpointIds,
        // Set<Long> sourceSystemVariableIds,
        Long targetSystemEndpointId,
        Long transformationRuleId,
        TransformationScriptDTO script
) {
}
