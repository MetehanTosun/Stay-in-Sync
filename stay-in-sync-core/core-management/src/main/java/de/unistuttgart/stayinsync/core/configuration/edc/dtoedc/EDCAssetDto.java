package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object (DTO) für EDC-Assets.
 * Dient zur Kommunikation zwischen Backend und Frontend.
 * Folgt dem Record-Pattern für Immutabilität und automatisches Generieren von Konstruktoren, 
 * equals, hashCode und toString.
 * 
 * Die Annotation @JsonProperty wird verwendet, um die Eigenschaften an das 
 * vom Frontend erwartete JSON-Format anzupassen.
 */
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
    /**
     * Alternativer Konstruktor, der automatisch einen Standardkontext hinzufügt.
     * Dies vereinfacht die Erstellung eines DTO ohne explizite Angabe des Kontexts.
     */
    public EDCAssetDto(UUID id, String assetId, String url, String type, String contentType, 
                      String description, UUID targetEDCId, EDCDataAddressDto dataAddress, 
                      EDCPropertyDto properties) {
        this(id, assetId, url, type, contentType, description, targetEDCId, dataAddress, properties, 
             new HashMap<>(Map.of("edc", "https://w3id.org/edc/v0.0.1/ns/")));
    }
    
    /**
     * Gibt eine String-Repräsentation des DTOs zurück.
     * Überschreibt die automatisch generierte toString-Methode für bessere Lesbarkeit.
     */
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
