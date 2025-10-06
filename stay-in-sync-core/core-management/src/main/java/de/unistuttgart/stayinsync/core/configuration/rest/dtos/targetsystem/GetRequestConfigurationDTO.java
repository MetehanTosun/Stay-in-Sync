package de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem;

import de.unistuttgart.stayinsync.transport.domain.TargetApiRequestConfigurationPatternType;

import java.util.List;

public record GetRequestConfigurationDTO(
        Long id,
        String alias,
        String targetSystemName,
        TargetApiRequestConfigurationPatternType arcPatternType,
        List<GetActionDTO> actions,
        String arcType
) {
}
