package de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem;

import de.unistuttgart.stayinsync.core.transport.domain.TargetApiRequestConfigurationPatternType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateArcDTO(
        @NotBlank String alias,
        @NotNull Long targetSystemId,
        @NotNull TargetApiRequestConfigurationPatternType arcPatternType,
        @NotNull List<ActionDefinitionDTO> actions
) {
}
