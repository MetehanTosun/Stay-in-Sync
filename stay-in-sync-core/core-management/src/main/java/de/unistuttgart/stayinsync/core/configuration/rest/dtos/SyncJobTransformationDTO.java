package de.unistuttgart.stayinsync.core.configuration.rest.dtos;


import de.unistuttgart.stayinsync.core.transport.domain.JobDeploymentStatus;

public record SyncJobTransformationDTO(
        Long id,
        String name,
        JobDeploymentStatus deploymentStatus,
        SyncJobTransformationRuleDTO transformationRule
) {
}
