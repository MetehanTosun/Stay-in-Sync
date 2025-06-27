package de.unistuttgart.stayinsync.transport.dto;

import java.util.Set;

public record SourceSystemEndpointMessageDTO(Long id, String endpointPath, String httpRequestType,
                                             boolean pollingActive,
                                             int pollingRateInMs,
                                             SourceSystemMessageDTO sourceSystem,
                                             Set<SourceSystemApiQueryParamMessageDTO> apiQueryParams,
                                             Set<SourceSystemApiRequestHeaderMessageDTO> apiRequestHeaders) {
}

