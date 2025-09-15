package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object (DTO) für EDC-Assets.
 * Dient zur Kommunikation zwischen Backend und Frontend.
 * Die Annotation @JsonProperty wird verwendet, um die Eigenschaften an das 
 * vom Frontend erwartete JSON-Format anzupassen.
 */
public class EDCAssetDto {
    
    /**
     * Die ID des DTOs, wird nicht in der JSON-Antwort enthalten sein.
     */
    private UUID id;

    /**
     * Die Asset-ID, wird als @id im JSON dargestellt.
     */
    @JsonProperty("@id")
    @JsonAlias({"assetId"})
    @NotBlank
    private String assetId;
    
    /**
     * Der Name des Assets.
     */
    private String name;
    
    /**
     * Die URL des Assets.
     */
    private String url;
    
    /**
     * Der Typ des Assets.
     */
    private String type;
    
    /**
     * Der Content-Type des Assets.
     */
    private String contentType;
    
    /**
     * Die Beschreibung des Assets.
     */
    private String description;
    
    /**
     * Die ID der Ziel-EDC-Instanz.
     */
    @JsonProperty("targetEDCId")
    private UUID targetEDCId;
    
    /**
     * Die Daten-Adresse des Assets.
     */
    @JsonProperty("dataAddress")
    @NotNull
    private EDCDataAddressDto dataAddress;
    
    /**
     * Die Eigenschaften des Assets.
     */
    @JsonProperty("properties")
    private List<EDCPropertyDto> properties;
    
    /**
     * Der Kontext des Assets, wird als @context im JSON dargestellt.
     */
    @JsonProperty("@context")
    private Map<String, String> context;
    
    /**
     * Default-Konstruktor.
     */
    public EDCAssetDto() {
        this.context = new HashMap<>(Map.of("edc", "https://w3id.org/edc/v0.0.1/ns/"));
    }
    
    /**
     * Konstruktor mit allen Pflichtfeldern.
     * 
     * @param id Die ID des DTOs
     * @param assetId Die Asset-ID
     * @param name Der Name des Assets
     * @param url Die URL des Assets
     * @param type Der Typ des Assets
     * @param contentType Der Content-Type des Assets
     * @param description Die Beschreibung des Assets
     * @param targetEDCId Die ID der Ziel-EDC-Instanz
     * @param dataAddress Die Daten-Adresse des Assets
     * @param properties Die Eigenschaften des Assets
     */
    public EDCAssetDto(UUID id, String assetId, String name, String url, String type, String contentType, 
                      String description, UUID targetEDCId, EDCDataAddressDto dataAddress, 
                      List<EDCPropertyDto> properties) {
        this();
        this.id = id;
        this.assetId = assetId;
        this.name = name;
        this.url = url;
        this.type = type;
        this.contentType = contentType;
        this.description = description;
        this.targetEDCId = targetEDCId;
        this.dataAddress = dataAddress;
        this.properties = properties;
    }
    
    /**
     * Vollständiger Konstruktor.
     * 
     * @param id Die ID des DTOs
     * @param assetId Die Asset-ID
     * @param name Der Name des Assets
     * @param url Die URL des Assets
     * @param type Der Typ des Assets
     * @param contentType Der Content-Type des Assets
     * @param description Die Beschreibung des Assets
     * @param targetEDCId Die ID der Ziel-EDC-Instanz
     * @param dataAddress Die Daten-Adresse des Assets
     * @param properties Die Eigenschaften des Assets
     * @param context Der Kontext des Assets
     */
    public EDCAssetDto(UUID id, String assetId, String name, String url, String type, String contentType, 
                      String description, UUID targetEDCId, EDCDataAddressDto dataAddress, 
                      List<EDCPropertyDto> properties, Map<String, String> context) {
        this.id = id;
        this.assetId = assetId;
        this.name = name;
        this.url = url;
        this.type = type;
        this.contentType = contentType;
        this.description = description;
        this.targetEDCId = targetEDCId;
        this.dataAddress = dataAddress;
        this.properties = properties;
        this.context = context != null ? context : new HashMap<>(Map.of("edc", "https://w3id.org/edc/v0.0.1/ns/"));
    }
    
    // Getter und Setter
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getAssetId() {
        return assetId;
    }
    
    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public UUID getTargetEDCId() {
        return targetEDCId;
    }
    
    public void setTargetEDCId(UUID targetEDCId) {
        this.targetEDCId = targetEDCId;
    }
    
    public EDCDataAddressDto getDataAddress() {
        return dataAddress;
    }
    
    public void setDataAddress(EDCDataAddressDto dataAddress) {
        this.dataAddress = dataAddress;
    }
    
    public List<EDCPropertyDto> getProperties() {
        return properties;
    }
    
    public void setProperties(List<EDCPropertyDto> properties) {
        this.properties = properties;
    }
    
    public Map<String, String> getContext() {
        return context;
    }
    
    public void setContext(Map<String, String> context) {
        this.context = context != null ? context : new HashMap<>(Map.of("edc", "https://w3id.org/edc/v0.0.1/ns/"));
    }
    
    /**
     * Gibt eine String-Repräsentation des DTOs zurück.
     * 
     * @return Eine lesbare Darstellung des DTOs
     */
    @Override
    public String toString() {
        return "EDCAssetDto{" +
                "id=" + id +
                ", assetId='" + assetId + '\'' +
                ", name='" + name + '\'' +
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
