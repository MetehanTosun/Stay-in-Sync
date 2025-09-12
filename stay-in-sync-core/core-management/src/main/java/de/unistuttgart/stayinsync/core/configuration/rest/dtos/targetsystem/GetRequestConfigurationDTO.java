package de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem;

import de.unistuttgart.stayinsync.core.transport.domain.TargetApiRequestConfigurationPatternType;

import java.util.List;

public record GetRequestConfigurationDTO(
        Long id,
        String alias,
        String targetSystemName,
        TargetApiRequestConfigurationPatternType arcPatternType,
        List<GetActionDTO> actions
) {
}
