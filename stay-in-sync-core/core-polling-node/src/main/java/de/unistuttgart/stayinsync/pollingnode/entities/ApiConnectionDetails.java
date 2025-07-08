package de.unistuttgart.stayinsync.pollingnode.entities;

import de.unistuttgart.stayinsync.transport.dto.ApiRequestHeaderMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.ApiRequestParameterMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemEndpointMessageDTO;
import de.unistuttgart.stayinsync.transport.dto.SourceSystemMessageDTO;

import java.util.Objects;
import java.util.Set;

public record ApiConnectionDetails(SourceSystemMessageDTO sourceSystem, SourceSystemEndpointMessageDTO endpoint,  Set<ApiRequestParameterMessageDTO> requestParameters,
                                   Set<ApiRequestHeaderMessageDTO> requestHeader) {



}
