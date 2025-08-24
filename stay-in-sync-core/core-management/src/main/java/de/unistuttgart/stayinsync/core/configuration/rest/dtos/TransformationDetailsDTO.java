package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.SourceSystemApiRequestConfiguration;

import java.util.Set;

import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;

public record TransformationDetailsDTO(
        Long id,
        String name,
        String description,
        Long syncJobId,
//        Set<Long> sourceSystemEndpointIds,
        // Set<Long> sourceSystemVariableIds,
        JobDeploymentStatus deploymentStatus,
        Long transformationRuleId,
        TransformationScriptDTO script,
        Set<SourceSystemApiRequestConfiguration> sourceSystemApiRequestConfigurations
) {
}
