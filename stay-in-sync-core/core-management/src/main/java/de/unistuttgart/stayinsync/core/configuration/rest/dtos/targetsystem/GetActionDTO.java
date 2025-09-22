package de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem;

import de.unistuttgart.stayinsync.core.transport.domain.TargetApiRequestConfigurationActionRole;

public record GetActionDTO(
        Long endpointId,
        String endpointPath,
        String httpMethod,
        TargetApiRequestConfigurationActionRole actionRole,
        int executionOrder
) {
}
