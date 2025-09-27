package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;
import de.unistuttgart.stayinsync.transport.dto.ApiRequestParameterMessageDTO;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record GetRequestConfigurationDTO(
        Long id,
        @NotNull String alias,
        Long endpointId,
        String sourceSystemName,
        boolean active,
        int pollingIntervallTimeInMs,
        Set<ApiHeaderDTO> apiRequestHeaders,
        Set<ApiRequestParameterMessageDTO> apiRequestParameters,
        JobDeploymentStatus deploymentStatus,
        String responseDts
) {
}
