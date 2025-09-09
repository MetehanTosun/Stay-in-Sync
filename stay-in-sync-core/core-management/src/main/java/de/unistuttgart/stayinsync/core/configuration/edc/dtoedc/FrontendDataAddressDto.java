package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

/**
 * Repräsentiert das DataAddress-Format aus dem Frontend
 */
public record FrontendDataAddressDto(
        String type, String baseUrl
) {
}
