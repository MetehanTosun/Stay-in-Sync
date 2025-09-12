package de.unistuttgart.stayinsync.core.transport.dto;

public record SourceSystemMessageDTO(String name, String apiUrl, String apiType,
                                     ApiAuthConfigurationMessageDTO authDetails) {


}
