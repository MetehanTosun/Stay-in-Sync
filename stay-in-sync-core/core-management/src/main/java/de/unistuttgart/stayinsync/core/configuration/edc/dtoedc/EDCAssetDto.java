package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record EDCAssetDto(
        @JsonIgnore // ID wird nicht in der JSON-Antwort enthalten sein
        UUID id,

        @JsonProperty("@id") // Mappe die assetId auf @id für das Frontend
        @NotBlank
        String assetId,

        // Diese Felder werden für das Frontend benötigt
        String url,

        String type,

        String contentType,

        String description,

        @JsonProperty("targetEDCId") // Mappe auf targetEDCId für das Frontend
        UUID targetEDCId,

        @JsonProperty("dataAddress") // Mappe auf dataAddress für das Frontend
        @NotNull
        EDCDataAddressDto dataAddress,

        @JsonProperty("properties") // Mappe auf properties für das Frontend
        EDCPropertyDto properties,
        
        @JsonProperty("@context") // Kontext für EDC Frontend
        Map<String, String> context
) {
    // Konstruktor mit Standardwerten für den Kontext
    public EDCAssetDto(UUID id, String assetId, String url, String type, String contentType, 
                      String description, UUID targetEDCId, EDCDataAddressDto dataAddress, 
                      EDCPropertyDto properties) {
        this(id, assetId, url, type, contentType, description, targetEDCId, dataAddress, properties, 
             new HashMap<>(Map.of("edc", "https://w3id.org/edc/v0.0.1/ns/")));
    }
    
    @Override
    public String toString() {
        return "EDCAssetDto{" +
                "id=" + id +
                ", assetId='" + assetId + '\'' +
                ", url='" + url + '\'' +
                ", type='" + type + '\'' +
                ", contentType='" + contentType + '\'' +
                ", description='" + description + '\'' +
                ", targetEDCId=" + targetEDCId +
                ", dataAddress=" + dataAddress +
                ", properties=" + properties +
                '}';
    }
}
