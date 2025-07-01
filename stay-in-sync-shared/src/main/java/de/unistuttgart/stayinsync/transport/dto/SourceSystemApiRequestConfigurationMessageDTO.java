package de.unistuttgart.stayinsync.transport.dto;

import java.util.Set;

public record SourceSystemApiRequestConfigurationMessageDTO(Long id, SourceSystemMessageDTO sourceSystem,
                                                            SourceSystemEndpointMessageDTO endpoint, boolean active,
                                                            Set<ApiRequestParameterMessageDTO> requestParameters,
                                                            Set<ApiRequestHeaderMessageDTO> requestHeader) {
}
