package de.unistuttgart.stayinsync.core.transport.dto;

import de.unistuttgart.stayinsync.core.transport.domain.JobDeploymentStatus;

public record SourceSystemApiRequestConfigurationMessageDTO(String name, Long id, int pollingIntervallTimeInMs, boolean active, JobDeploymentStatus deploymentStatus,
                                                            ApiConnectionDetailsDTO apiConnectionDetails) {
}
