package de.unistuttgart.stayinsync.core.configuration.rest.dtos;


import de.unistuttgart.stayinsync.core.transport.domain.JobDeploymentStatus;

public record TransformationStatusUpdate(Long transformationId, Long syncJobId, JobDeploymentStatus deploymentStatus) {
}
