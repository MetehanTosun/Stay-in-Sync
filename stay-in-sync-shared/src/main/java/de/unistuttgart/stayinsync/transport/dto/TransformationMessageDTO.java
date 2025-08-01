package de.unistuttgart.stayinsync.transport.dto;

import de.unistuttgart.stayinsync.transport.dto.targetsystems.RequestConfigurationMessageDTO;

import java.util.List;
import java.util.Set;

public record TransformationMessageDTO(
        Long id,
        TransformationScriptDTO transformationScriptDTO,
        TransformationRuleDTO transformationRuleDTO,
        Set<SourceSystemApiRequestConfigurationMessageDTO> requestConfigurationMessageDTOS,
        List<String> arcManifest,
        Set<RequestConfigurationMessageDTO> targetRequestConfigurationMessageDTOS
) {
}
