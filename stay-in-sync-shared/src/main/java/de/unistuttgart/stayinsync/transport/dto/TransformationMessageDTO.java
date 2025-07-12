package de.unistuttgart.stayinsync.transport.dto;

import java.util.Set;

public record TransformationMessageDTO(TransformationRuleDTO transformationRuleDTO,
                                       Set<SourceSystemApiRequestConfigurationMessageDTO> requestConfigurationMessageDTOS) {
}
