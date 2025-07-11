package de.unistuttgart.stayinsync.transport.dto;

import java.util.Set;

public record ApiConnectionDetailsDTO(SourceSystemMessageDTO sourceSystem, SourceSystemEndpointMessageDTO endpoint, Set<ApiRequestParameterMessageDTO> requestParameters,
                                      Set<ApiRequestHeaderMessageDTO> requestHeader) {
}
