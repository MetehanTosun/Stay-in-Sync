package de.unistuttgart.stayinsync.transport.dto;

import de.unistuttgart.stayinsync.transport.domain.JobDeploymentStatus;

import java.util.List;
import java.util.Set;

public record TransformationMessageDTO(
        Long id,
        String name,
        TransformationScriptDTO transformationScriptDTO,
        TransformationRuleDTO transformationRuleDTO,
        JobDeploymentStatus jobDeploymentStatus,
        Set<SourceSystemApiRequestConfigurationMessageDTO> requestConfigurationMessageDTOS,
        List<String> arcManifest // TODO: When building TransformationmessageDTO, add manifest with iterate over ARCs
) {
}
