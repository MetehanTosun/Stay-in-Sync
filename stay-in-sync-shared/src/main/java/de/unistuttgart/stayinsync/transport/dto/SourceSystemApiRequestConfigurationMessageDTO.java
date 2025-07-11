package de.unistuttgart.stayinsync.transport.dto;

public record SourceSystemApiRequestConfigurationMessageDTO(String name, Long id, int pollingIntervallTimeInMs, boolean active,
                                                            ApiConnectionDetailsDTO apiConnectionDetails) {
}
