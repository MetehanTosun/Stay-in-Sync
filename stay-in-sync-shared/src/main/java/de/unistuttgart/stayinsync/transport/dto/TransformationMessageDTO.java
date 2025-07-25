package de.unistuttgart.stayinsync.transport.dto;

import java.util.List;
import java.util.Set;

public record TransformationMessageDTO(
        Long id,
        TransformationScriptDTO transformationScriptDTO,
        TransformationRuleDTO transformationRuleDTO,
        Set<SourceSystemApiRequestConfigurationMessageDTO> requestConfigurationMessageDTOS,
        List<String> arcManifest // TODO: When building TransformationmessageDTO, add manifest with iterate over ARCs
) {
}
