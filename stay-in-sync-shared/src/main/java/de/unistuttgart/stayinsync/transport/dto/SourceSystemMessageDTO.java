package de.unistuttgart.stayinsync.transport.dto;

public record SourceSystemMessageDTO(String name, String apiUrl, String apiType,
                                     ApiAuthConfigurationMessageDTO authDetails) {
}
