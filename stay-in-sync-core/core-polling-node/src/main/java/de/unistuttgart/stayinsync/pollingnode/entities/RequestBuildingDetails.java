package de.unistuttgart.stayinsync.pollingnode.entities;


import de.unistuttgart.stayinsync.core.transport.dto.ApiRequestHeaderMessageDTO;
import de.unistuttgart.stayinsync.core.transport.dto.ApiRequestParameterMessageDTO;
import de.unistuttgart.stayinsync.core.transport.dto.SourceSystemEndpointMessageDTO;
import de.unistuttgart.stayinsync.core.transport.dto.SourceSystemMessageDTO;

import java.util.Set;

public record RequestBuildingDetails(SourceSystemMessageDTO sourceSystem, SourceSystemEndpointMessageDTO endpoint, Set<ApiRequestParameterMessageDTO> requestParameters,
                                     Set<ApiRequestHeaderMessageDTO> requestHeader) {
}
