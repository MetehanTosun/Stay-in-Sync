package de.unistuttgart.stayinsync.transport.dto;

public record SourceSystemApiRequestConfigurationMessageDTO(Long id, int pollingIntervallTimeInMs, boolean active,
                                                            ApiConnectionDetailsDTO apiConnectionDetails) {
}
