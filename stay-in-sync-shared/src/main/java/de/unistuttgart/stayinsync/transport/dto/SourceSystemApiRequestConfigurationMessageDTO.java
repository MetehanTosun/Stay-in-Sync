package de.unistuttgart.stayinsync.transport.dto;

import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;

public record SourceSystemApiRequestConfigurationMessageDTO(String name, Long id, int pollingIntervallTimeInMs, boolean active, JobDeploymentStatus deploymentStatus,
                                                            ApiConnectionDetailsDTO apiConnectionDetails) {
}
