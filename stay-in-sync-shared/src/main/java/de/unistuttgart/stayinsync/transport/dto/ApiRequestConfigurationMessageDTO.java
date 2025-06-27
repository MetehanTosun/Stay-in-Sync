package de.unistuttgart.stayinsync.transport.dto;

import java.util.Set;

public record ApiRequestConfigurationMessageDTO(Long id, SourceSystemMessageDTO sourceSystemMessageDTO,
                                                SourceSystemEndpointMessageDTO endpoint, boolean active,
                                                Set<ApiRequestParameterMessageDTO> requestParameterMessages,
                                                Set<ApiRequestHeaderMessageDTO> requestHeaderMessages) {
}
