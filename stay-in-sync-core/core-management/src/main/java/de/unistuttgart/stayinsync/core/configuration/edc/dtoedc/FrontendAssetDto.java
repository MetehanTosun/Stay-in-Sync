package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.UUID;

/**
 * Repräsentiert das Asset-Format wie es vom Frontend erwartet und geliefert wird.
 * Diese Klasse dient als Zwischenschicht zur Konvertierung zwischen dem Frontend-Format 
 * und dem im Backend verwendeten Format (EDCAssetDto).
 * 
 * Der Hauptunterschied liegt in der Struktur der Properties und der Darstellung von Metadaten.
 */
public class FrontendAssetDto {
    
    /**
     * Der JSON-LD Kontext für das Asset.
     * Typischerweise enthält dies die EDC-Namespace-Definition.
     */
    @JsonProperty("@context")
    private Map<String, String> context;
    
    /**
     * Die eindeutige ID des Assets.
     * Im JSON-LD Format als @id dargestellt.
     */
    @JsonProperty("@id")
    private String id;
    
    /**
     * Die Eigenschaften des Assets als flache Map.
     * Im Frontend-Format werden alle Properties in einer einfachen Map abgelegt,
     * während das Backend eine strukturierte Darstellung verwendet.
     */
    private Map<String, String> properties;
    
    /**
     * Die Adresse, unter der die Asset-Daten erreichbar sind.
     * Format ist auf die Frontend-Bedürfnisse angepasst.
     */
    private FrontendDataAddressDto dataAddress;
    
    // Getter und Setter mit Method Chaining für Fluent API
    
    public Map<String, String> getContext() {
        return context;
    }
    
    public FrontendAssetDto setContext(Map<String, String> context) {
        this.context = context;
        return this;
    }
    
    public String getId() {
        return id;
    }
    
    public FrontendAssetDto setId(String id) {
        this.id = id;
        return this;
    }
    
    public Map<String, String> getProperties() {
        return properties;
    }
    
    public FrontendAssetDto setProperties(Map<String, String> properties) {
        this.properties = properties;
        return this;
    }
    
    public FrontendDataAddressDto getDataAddress() {
        return dataAddress;
    }
    
    public FrontendAssetDto setDataAddress(FrontendDataAddressDto dataAddress) {
        this.dataAddress = dataAddress;
        return this;
    }
    
    /**
     * Konvertiert das Frontend-Format in das Backend-Format.
     * 
     * @param targetEDCId Die ID der Ziel-EDC-Instanz, mit der dieses Asset verknüpft werden soll
     * @return Ein EDCAssetDto im Backend-Format
     */
    public EDCAssetDto toBackendDto(UUID targetEDCId) {
        // Extrahiere Werte aus den Frontend-Properties
        String assetId = this.id != null && !this.id.isEmpty() ? this.id : null;
        String description = this.properties != null ? this.properties.get("asset:prop:description") : null;
        String contentType = this.properties != null ? this.properties.get("asset:prop:contenttype") : "application/json";
        
        // Konvertiere die Datenadresse
        EDCDataAddressDto dataAddressDto = null;
        if (this.dataAddress != null) {
            dataAddressDto = new EDCDataAddressDto()
                    .setJsonLDType("DataAddress")
                    .setType(this.dataAddress.getType())
                    .setBaseURL(this.dataAddress.getBaseUrl())
                    .setProxyPath(true)
                    .setProxyQueryParams(true);
        }
        
        // Erstelle Properties-Objekt
        EDCPropertyDto propertyDto = null;
        if (description != null) {
            propertyDto = new EDCPropertyDto()
                    .setDescription(description);
        }
        
        // Erstelle und gib das Backend-DTO zurück
        return new EDCAssetDto(
                null, // ID wird automatisch generiert
                assetId != null ? assetId : "asset-" + UUID.randomUUID().toString(),
                this.dataAddress != null ? this.dataAddress.getBaseUrl() : "",
                this.dataAddress != null ? this.dataAddress.getType() : "HttpData",
                contentType != null ? contentType : "application/json",
                description,
                targetEDCId,
                dataAddressDto,
                propertyDto
        );
    }
    
    
    /**
     * Konvertiert ein Backend-DTO in das Frontend-Format.
     * Diese statische Factory-Methode erstellt ein neues Frontend-DTO aus einem Backend-DTO.
     * 
     * @param backendDto Das Backend-DTO, das konvertiert werden soll
     * @return Ein neues FrontendAssetDto mit Daten aus dem Backend-DTO
     */
    public static FrontendAssetDto fromBackendDto(EDCAssetDto backendDto) {
        FrontendAssetDto frontendDto = new FrontendAssetDto();
        
        // Standard-EDC-Kontext setzen
        Map<String, String> context = Map.of("edc", "https://w3id.org/edc/v0.0.1/ns/");
        frontendDto.setContext(context);
        
        // Asset-ID übernehmen
        frontendDto.setId(backendDto.assetId());
        
        // Properties in das Frontend-Format konvertieren
        // Im Frontend werden alle Eigenschaften in einer flachen Map gespeichert
        Map<String, String> properties = Map.of(
                "asset:prop:name", backendDto.assetId(),
                "asset:prop:description", backendDto.description() != null ? backendDto.description() : "",
                "asset:prop:contenttype", backendDto.contentType(),
                "asset:prop:version", "1.0.0"
        );
        frontendDto.setProperties(properties);
        
        // DataAddress in das Frontend-Format konvertieren
        FrontendDataAddressDto dataAddress = new FrontendDataAddressDto()
                .setType(backendDto.dataAddress().getType())
                .setBaseUrl(backendDto.dataAddress().getBaseURL());
        frontendDto.setDataAddress(dataAddress);
        
        return frontendDto;
    }
}