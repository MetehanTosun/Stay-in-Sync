package de.unistuttgart.stayinsync.core.transport.dto;

import de.unistuttgart.stayinsync.core.transport.domain.JobDeploymentStatus;

public record TransformationDeploymentFeedbackMessageDTO(JobDeploymentStatus status, Long transformationId, String syncNode) {
}
