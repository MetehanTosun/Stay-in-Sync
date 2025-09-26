package de.unistuttgart.stayinsync.core.configuration.rest.dtos;


import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;

public record TransformationStatusUpdate(Long transformationId, Long syncJobId, JobDeploymentStatus deploymentStatus) {
}
