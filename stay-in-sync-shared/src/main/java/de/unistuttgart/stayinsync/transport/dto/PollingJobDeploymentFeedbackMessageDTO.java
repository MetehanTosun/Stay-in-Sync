package de.unistuttgart.stayinsync.transport.dto;

import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;

public record PollingJobDeploymentFeedbackMessageDTO(JobDeploymentStatus deploymentStatus, Long requestConfigId, String pollingPod) {
}
