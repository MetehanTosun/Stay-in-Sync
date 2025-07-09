package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import java.util.Set;

public record TransformationAssemblyDTO(
        Long id,
        Long syncJobId,
        Set<Long> sourceSystemEndpointIds,
// Set<Long> sourceSystemVariableIds,
        Long targetSystemEndpointId,
        Long transformationRuleId,
        Long transformationScriptId
) {
}
