package de.unistuttgart.stayinsync.core.transport.dto;

import de.unistuttgart.stayinsync.core.transport.domain.JobDeploymentStatus;

public record PollingJobDeploymentFeedbackMessageDTO(JobDeploymentStatus deploymentStatus, Long requestConfigId, String pollingPod) {
}
