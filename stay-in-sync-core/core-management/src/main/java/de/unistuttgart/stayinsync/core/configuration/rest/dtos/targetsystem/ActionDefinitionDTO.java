package de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem;

import de.unistuttgart.stayinsync.core.transport.domain.TargetApiRequestConfigurationActionRole;
import jakarta.validation.constraints.NotNull;

public record ActionDefinitionDTO(
        @NotNull Long endpointId,
        @NotNull TargetApiRequestConfigurationActionRole actionRole,
        @NotNull Integer executionOrder
) {
}
