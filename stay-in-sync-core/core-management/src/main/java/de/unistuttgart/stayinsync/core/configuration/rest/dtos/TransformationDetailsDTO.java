package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

public record TransformationDetailsDTO(
        Long id,
        String name,
        String description,
        Long syncJobId,
//        Set<Long> sourceSystemEndpointIds,
        // Set<Long> sourceSystemVariableIds,
        Long targetSystemEndpointId,
        Long transformationRuleId,
        TransformationScriptDTO script
) {
}
