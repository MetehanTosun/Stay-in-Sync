package de.unistuttgart.stayinsync.transport.dto;

import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;

public record TransformationDeploymentFeedbackMessageDTO(JobDeploymentStatus status, Long transformationId, String syncNode) {
}
