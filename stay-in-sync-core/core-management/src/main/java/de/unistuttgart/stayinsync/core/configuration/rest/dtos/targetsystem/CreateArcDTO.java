package de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem;

import de.unistuttgart.stayinsync.transport.domain.TargetApiRequestConfigurationPatternType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record CreateArcDTO(
        @NotBlank String alias,
        @NotNull Long targetSystemId,
        @NotNull TargetApiRequestConfigurationPatternType arcPatternType,
        @NotNull List<ActionDefinitionDTO> actions,
        Map<String, String> staticHeaderValues
) {
}
