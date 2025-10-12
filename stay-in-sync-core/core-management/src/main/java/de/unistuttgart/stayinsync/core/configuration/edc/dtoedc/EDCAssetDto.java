package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object (DTO) für EDC-Assets.
 * Dient zur Kommunikation zwischen Backend und Frontend.
 * Die Klasse ist flexibel gestaltet, um verschiedene JSON-Formate zu unterstützen:
 * - Das vollständige EDC-Format mit allen erforderlichen Feldern
 * - Ein vereinfachtes Format für einfachere Client-Anwendungen
 * 
 * Die Konvertierung zwischen den Formaten erfolgt automatisch.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EDCAssetDto {
    
    /**
     * Die ID des DTOs, wird nicht in der JSON-Antwort enthalten sein.
     */
    @JsonIgnore
    private UUID id;

    /**
     * Die Asset-ID, wird als @id im JSON dargestellt.
     */
    @JsonProperty("@id")
    @JsonAlias({"assetId"})
    @NotBlank
    private String assetId;
    
    /**
     * Der JSON-LD Typ des Assets, standardmäßig "Asset".
     */
    @JsonProperty("@type")
    private String jsonLDType = "Asset";
    
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
     * Die Eigenschaften des Assets als Map. Der EDC erwartet die Properties als Map von Schlüssel-Wert-Paaren.
     */
    @JsonProperty("properties")
    private Map<String, Object> properties = new HashMap<>();
    
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
        this.properties = new HashMap<>();
        //Standardwerte für die Properties initialisieren
        initializeDefaultProperties();
    }
    
    /**
     * Initialisiert die Standard-Properties für das Asset.
     */
    private void initializeDefaultProperties() {
        if (name != null) {
            properties.put("asset:prop:name", name);
        }
        if (description != null) {
            properties.put("asset:prop:description", description);
        }
        if (contentType != null) {
            properties.put("asset:prop:contenttype", contentType);
        }
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
     * @param properties Die Eigenschaften des Assets als Map
     */
    public EDCAssetDto(UUID id, String assetId, String name, String url, String type, String contentType, 
                      String description, UUID targetEDCId, EDCDataAddressDto dataAddress, 
                      Map<String, Object> properties) {
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
        if (properties != null) {
            this.properties = properties;
        }
        // Stellen Sie sicher, dass die Properties mit den Attributen synchronisiert sind
        initializeDefaultProperties();
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
     * @param properties Die Eigenschaften des Assets als Map
     * @param context Der Kontext des Assets
     */
    public EDCAssetDto(UUID id, String assetId, String name, String url, String type, String contentType, 
                      String description, UUID targetEDCId, EDCDataAddressDto dataAddress, 
                      Map<String, Object> properties, Map<String, String> context) {
        this.id = id;
        this.assetId = assetId;
        this.name = name;
        this.url = url;
        this.type = type;
        this.contentType = contentType;
        this.description = description;
        this.targetEDCId = targetEDCId;
        this.dataAddress = dataAddress;
        this.properties = properties != null ? properties : new HashMap<>();
        this.context = context != null ? context : new HashMap<>(Map.of("edc", "https://w3id.org/edc/v0.0.1/ns/"));
        // Stellen Sie sicher, dass die Properties mit den Attributen synchronisiert sind
        initializeDefaultProperties();
    }
    
        /**
     * Konstruktor mit allen Feldern.
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
     */
    public EDCAssetDto(UUID id, String assetId, String name, String url, String type, String contentType, 
                      String description, UUID targetEDCId, EDCDataAddressDto dataAddress) {
        this.id = id;
        this.assetId = assetId;
        this.name = name;
        this.url = url;
        this.type = type;
        this.contentType = contentType;
        this.description = description;
        this.targetEDCId = targetEDCId;
        this.dataAddress = dataAddress;
        this.properties = new HashMap<>();
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
    
    public String getJsonLDType() {
        return jsonLDType;
    }
    
    public void setJsonLDType(String jsonLDType) {
        this.jsonLDType = jsonLDType;
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
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties != null ? properties : new HashMap<>();
    }
    
    /**
     * Hilfsmethode zum Hinzufügen einer Eigenschaft.
     * 
     * @param key Der Schlüssel der Eigenschaft
     * @param value Der Wert der Eigenschaft
     * @return Das DTO selbst für Method Chaining
     */
    public EDCAssetDto addProperty(String key, Object value) {
        if (this.properties == null) {
            this.properties = new HashMap<>();
        }
        this.properties.put(key, value);
        return this;
    }
    
    // Removed setPropertiesList method as we now use map-based properties directly
    
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
    /**
     * Konvertiert dieses DTO in ein vereinfachtes Format, das für einfache Client-Anwendungen
     * besser geeignet ist. Das vereinfachte Format hat eine flachere Struktur.
     * 
     * @return Eine Map mit dem vereinfachten Format
     */
    public Map<String, Object> toSimplifiedFormat() {
        Map<String, Object> simplified = new HashMap<>();
        
        // Kontext und ID übernehmen
        simplified.put("@context", this.context);
        simplified.put("@id", this.assetId);
        
        // Properties als eigene Map
        Map<String, String> simpleProps = new HashMap<>();
        if (this.properties != null) {
            for (Map.Entry<String, Object> entry : this.properties.entrySet()) {
                simpleProps.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        simplified.put("properties", simpleProps);
        
        // Vereinfachte DataAddress
        if (this.dataAddress != null) {
            Map<String, Object> simpleAddress = new HashMap<>();
            simpleAddress.put("type", this.dataAddress.getType());
            simpleAddress.put("baseUrl", this.dataAddress.getBaseURL());
            simplified.put("dataAddress", simpleAddress);
        }
        
        return simplified;
    }
    
    /**
     * Erstellt ein EDCAssetDto aus einem vereinfachten Format.
     * 
     * @param simplified Die vereinfachte Map-Darstellung des Assets
     * @param targetEDCId Die ID der Ziel-EDC-Instanz
     * @return Ein neues EDCAssetDto
     */
    public static EDCAssetDto fromSimplifiedFormat(Map<String, Object> simplified, UUID targetEDCId) {
        EDCAssetDto dto = new EDCAssetDto();
        
        // Basis-Eigenschaften setzen
        if (simplified.containsKey("@id")) {
            dto.setAssetId((String) simplified.get("@id"));
        } else {
            dto.setAssetId("asset-" + UUID.randomUUID());
        }
        
        // Kontext übernehmen oder Standard setzen
        if (simplified.containsKey("@context") && simplified.get("@context") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> contextMap = (Map<String, String>) simplified.get("@context");
            dto.setContext(contextMap);
        }
        
        // Properties verarbeiten
        if (simplified.containsKey("properties") && simplified.get("properties") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> props = (Map<String, Object>) simplified.get("properties");
            for (Map.Entry<String, Object> entry : props.entrySet()) {
                dto.addProperty(entry.getKey(), entry.getValue());
                
                // Standard-Properties auch in die direkten Felder setzen
                if (entry.getKey().equals("asset:prop:name")) {
                    dto.setName((String) entry.getValue());
                } else if (entry.getKey().equals("asset:prop:description")) {
                    dto.setDescription((String) entry.getValue());
                } else if (entry.getKey().equals("asset:prop:contenttype")) {
                    dto.setContentType((String) entry.getValue());
                }
            }
        }
        
        // DataAddress verarbeiten
        if (simplified.containsKey("dataAddress") && simplified.get("dataAddress") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> addrMap = (Map<String, Object>) simplified.get("dataAddress");
            EDCDataAddressDto addr = new EDCDataAddressDto();
            
            if (addrMap.containsKey("type")) {
                addr.setType((String) addrMap.get("type"));
            }
            
            if (addrMap.containsKey("baseUrl")) {
                addr.setBaseURL((String) addrMap.get("baseUrl"));
            }
            
            // Standard-Werte setzen
            addr.setJsonLDType("DataAddress");
            addr.setProxyPath(true);
            addr.setProxyQueryParams(true);
            
            dto.setDataAddress(addr);
        }
        
        dto.setTargetEDCId(targetEDCId);
        
        return dto;
    }

    @Override
    public String toString() {
        return "EDCAssetDto{" +
                "id=" + id +
                ", assetId='" + assetId + '\'' +
                ", jsonLDType='" + jsonLDType + '\'' +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", type='" + type + '\'' +
                ", contentType='" + contentType + '\'' +
                ", description='" + description + '\'' +
                ", targetEDCId=" + targetEDCId +
                ", dataAddress=" + dataAddress +
                ", properties=" + properties +
                ", context=" + context +
                '}';
    }
}
