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

/**
 * Data Transfer Object (DTO) für EDC-Assets.
 * Dient zur Kommunikation zwischen Backend und Frontend.
 * Die Klasse ist flexibel gestaltet, um verschiedene JSON-Formate zu unterstützen:
 * - Das vollständige EDC-Format mit allen erforderlichen Feldern
 * - Ein vereinfachtes Format für einfachere Client-Anwendungen
 * 
 * Die Konvertierung zwischen den Formaten erfolgt automatisch.
 * Implementiert als Record für mehr Effizienz und bessere Lesbarkeit.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EDCAssetDto(
    /**
     * Die ID des DTOs, wird nicht in der JSON-Antwort enthalten sein.
     */
    @JsonIgnore
    Long id,

    /**
     * Die Asset-ID, wird als @id im JSON dargestellt.
     */
    @JsonProperty("@id")
    @JsonAlias({"assetId"})
    @NotBlank
    String assetId,
    
    /**
     * Der JSON-LD Typ des Assets, standardmäßig "Asset".
     */
    @JsonProperty("@type")
    String jsonLDType,
    
    /**
     * Der Name des Assets.
     * @JsonIgnore verhindert die direkte Serialisierung auf Root-Ebene - wird nur in properties gespeichert
     */
    @JsonIgnore
    String name,
    
    /**
     * Die URL des Assets.
     * @JsonIgnore verhindert die direkte Serialisierung auf Root-Ebene - wird nur über dataAddress gespeichert
     */
    @JsonIgnore
    String url,
    
    /**
     * Der Typ des Assets.
     * @JsonIgnore verhindert die direkte Serialisierung auf Root-Ebene - wird nur über dataAddress gespeichert
     */
    @JsonIgnore
    String type,
    
    /**
     * Der Content-Type des Assets.
     * @JsonIgnore verhindert die direkte Serialisierung auf Root-Ebene - wird nur in properties gespeichert
     */
    @JsonIgnore
    String contentType,
    
    /**
     * Die Beschreibung des Assets.
     * @JsonIgnore verhindert die direkte Serialisierung auf Root-Ebene - wird nur in properties gespeichert
     */
    @JsonIgnore
    String description,
    
    /**
     * Die ID der Ziel-EDC-Instanz.
     */
    @JsonProperty("targetEDCId")
    Long targetEDCId,
    
    /**
     * Die Daten-Adresse des Assets.
     */
    EDCDataAddressDto dataAddress,
    
    /**
     * Die Properties des Assets als Map.
     */
    Map<String, Object> properties,
    
    /**
     * Der Kontext des Assets.
     */
    @JsonProperty("@context")
    Map<String, String> context
) {
    /**
     * Default-Konstruktor.
     * Erstellt ein leeres Asset mit Standardwerten.
     */
    public EDCAssetDto() {
        this(null, "", "Asset", null, null, null, null, null, null, null, 
             new HashMap<>(), null);
    }
    
    /**
     * Konstruktor mit minimalen erforderlichen Parametern.
     * 
     * @param assetId Die Asset-ID
     * @param name Der Name des Assets
     */
    public EDCAssetDto(String assetId, String name) {
        this(null, assetId, "Asset", name, null, null, null, null, null, null,
             new HashMap<>(), null);
    }
    
    /**
     * Konstruktor mit den wichtigsten Parametern.
     * 
     * @param assetId Die Asset-ID
     * @param name Der Name des Assets
     * @param description Die Beschreibung des Assets
     * @param targetEDCId Die ID der Ziel-EDC-Instanz
     * @param dataAddress Die Daten-Adresse des Assets
     */
    public EDCAssetDto(String assetId, String name, String description, Long targetEDCId, 
                      EDCDataAddressDto dataAddress) {
        this(null, assetId, "Asset", name, null, null, null, description, 
             targetEDCId, dataAddress, new HashMap<>(), null);
    }
    
    /**
     * Canonical constructor with validation and defensive copying.
     */
    public EDCAssetDto {
        // Default value for jsonLDType if null
        if (jsonLDType == null) {
            jsonLDType = "Asset";
        }
        
        // Defensive copy for mutable property
        if (properties == null) {
            properties = new HashMap<>();
        } else {
            properties = new HashMap<>(properties);
        }
        
        // Defensive copy for context if present
        if (context != null) {
            context = new HashMap<>(context);
        }
    }
    
    /**
     * Gibt alle Properties des Assets zurück.
     * Diese werden bei der JSON-Serialisierung als Top-Level-Eigenschaften eingefügt.
     * 
     * HINWEIS: Diese Methode ist deaktiviert, um eine doppelte Anzeige von Properties zu verhindern.
     * Alle Properties sollten nun ausschließlich im properties-Objekt enthalten sein.
     * 
     * @return Eine leere Map, um Duplizierung zu vermeiden
     */
    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        // Zurückgeben einer leeren Map, damit keine doppelten Properties auf Root-Ebene angezeigt werden
        return new HashMap<>();
    }
    
    /**
     * Erstellt ein neues Asset-DTO mit der hinzugefügten Property.
     * 
     * @param key Der Schlüssel der Property
     * @param value Der Wert der Property
     * @return Ein neues DTO mit der hinzugefügten Property
     */
    public EDCAssetDto withProperty(String key, Object value) {
        if (key == null || value == null) {
            return this;
        }
        
        Map<String, Object> newProps = new HashMap<>(this.properties);
        newProps.put(key, value);
        
        return new EDCAssetDto(
            this.id, this.assetId, this.jsonLDType, this.name, this.url,
            this.type, this.contentType, this.description, this.targetEDCId,
            this.dataAddress, newProps, this.context
        );
    }
    
    /**
     * Setzt eine Property.
     * Diese Methode wird von Jackson für die Deserialisierung benötigt.
     * 
     * @param name Der Name der Property
     * @param value Der Wert der Property
     */
    @JsonAnySetter
    public void setProperty(String name, Object value) {
        // This is needed for Jackson deserialization, but since properties is final,
        // we need to directly modify the map even though it violates immutability
        if (name != null && value != null) {
            properties.put(name, value);
        }
    }
    
    /**
     * Erstellt ein neues Asset-DTO mit der angegebenen ID.
     * 
     * @param id Die neue ID
     * @return Ein neues DTO mit der angegebenen ID
     */
    public EDCAssetDto withId(Long id) {
        return new EDCAssetDto(
            id, this.assetId, this.jsonLDType, this.name, this.url,
            this.type, this.contentType, this.description, this.targetEDCId,
            this.dataAddress, this.properties, this.context
        );
    }
    
    /**
     * Erstellt ein neues Asset-DTO mit der angegebenen Asset-ID.
     * 
     * @param assetId Die neue Asset-ID
     * @return Ein neues DTO mit der angegebenen Asset-ID
     */
    public EDCAssetDto withAssetId(String assetId) {
        return new EDCAssetDto(
            this.id, assetId, this.jsonLDType, this.name, this.url,
            this.type, this.contentType, this.description, this.targetEDCId,
            this.dataAddress, this.properties, this.context
        );
    }
    
    /**
     * Erstellt ein neues Asset-DTO mit dem angegebenen Namen.
     * Aktualisiert auch den entsprechenden Property-Wert.
     * 
     * @param name Der neue Name
     * @return Ein neues DTO mit dem angegebenen Namen
     */
    public EDCAssetDto withName(String name) {
        Map<String, Object> updatedProperties = new HashMap<>(this.properties);
        if (name != null) {
            updatedProperties.put("asset:prop:name", name);
        }
        
        return new EDCAssetDto(
            this.id, this.assetId, this.jsonLDType, name, this.url,
            this.type, this.contentType, this.description, this.targetEDCId,
            this.dataAddress, updatedProperties, this.context
        );
    }
    
    /**
     * Erstellt ein neues Asset-DTO mit der angegebenen URL.
     * 
     * @param url Die neue URL
     * @return Ein neues DTO mit der angegebenen URL
     */
    public EDCAssetDto withUrl(String url) {
        return new EDCAssetDto(
            this.id, this.assetId, this.jsonLDType, this.name, url,
            this.type, this.contentType, this.description, this.targetEDCId,
            this.dataAddress, this.properties, this.context
        );
    }
    
    /**
     * Erstellt ein neues Asset-DTO mit dem angegebenen Typ.
     * 
     * @param type Der neue Typ
     * @return Ein neues DTO mit dem angegebenen Typ
     */
    public EDCAssetDto withType(String type) {
        return new EDCAssetDto(
            this.id, this.assetId, this.jsonLDType, this.name, this.url,
            type, this.contentType, this.description, this.targetEDCId,
            this.dataAddress, this.properties, this.context
        );
    }
    
    /**
     * Erstellt ein neues Asset-DTO mit dem angegebenen Content-Type.
     * Aktualisiert auch den entsprechenden Property-Wert.
     * 
     * @param contentType Der neue Content-Type
     * @return Ein neues DTO mit dem angegebenen Content-Type
     */
    public EDCAssetDto withContentType(String contentType) {
        Map<String, Object> updatedProperties = new HashMap<>(this.properties);
        if (contentType != null) {
            updatedProperties.put("asset:prop:contenttype", contentType);
        }
        
        return new EDCAssetDto(
            this.id, this.assetId, this.jsonLDType, this.name, this.url,
            this.type, contentType, this.description, this.targetEDCId,
            this.dataAddress, updatedProperties, this.context
        );
    }
    
    /**
     * Erstellt ein neues Asset-DTO mit der angegebenen Beschreibung.
     * Aktualisiert auch den entsprechenden Property-Wert.
     * 
     * @param description Die neue Beschreibung
     * @return Ein neues DTO mit der angegebenen Beschreibung
     */
    public EDCAssetDto withDescription(String description) {
        Map<String, Object> updatedProperties = new HashMap<>(this.properties);
        if (description != null) {
            updatedProperties.put("asset:prop:description", description);
        }
        
        return new EDCAssetDto(
            this.id, this.assetId, this.jsonLDType, this.name, this.url,
            this.type, this.contentType, description, this.targetEDCId,
            this.dataAddress, updatedProperties, this.context
        );
    }
    
    /**
     * Erstellt ein neues Asset-DTO mit der angegebenen Ziel-EDC-ID.
     * 
     * @param targetEDCId Die neue Ziel-EDC-ID
     * @return Ein neues DTO mit der angegebenen Ziel-EDC-ID
     */
    public EDCAssetDto withTargetEDCId(Long targetEDCId) {
        return new EDCAssetDto(
            this.id, this.assetId, this.jsonLDType, this.name, this.url,
            this.type, this.contentType, this.description, targetEDCId,
            this.dataAddress, this.properties, this.context
        );
    }
    
    /**
     * Erstellt ein neues Asset-DTO mit der angegebenen Daten-Adresse.
     * 
     * @param dataAddress Die neue Daten-Adresse
     * @return Ein neues DTO mit der angegebenen Daten-Adresse
     */
    public EDCAssetDto withDataAddress(EDCDataAddressDto dataAddress) {
        return new EDCAssetDto(
            this.id, this.assetId, this.jsonLDType, this.name, this.url,
            this.type, this.contentType, this.description, this.targetEDCId,
            dataAddress, this.properties, this.context
        );
    }
    
    /**
     * Erstellt ein neues Asset-DTO mit den angegebenen Properties.
     * 
     * @param properties Die neuen Properties
     * @return Ein neues DTO mit den angegebenen Properties
     */
    public EDCAssetDto withProperties(Map<String, Object> properties) {
        return new EDCAssetDto(
            this.id, this.assetId, this.jsonLDType, this.name, this.url,
            this.type, this.contentType, this.description, this.targetEDCId,
            this.dataAddress, properties != null ? new HashMap<>(properties) : new HashMap<>(), 
            this.context
        );
    }
    
    /**
     * Erstellt ein neues Asset-DTO mit dem angegebenen Kontext.
     * 
     * @param context Der neue Kontext
     * @return Ein neues DTO mit dem angegebenen Kontext
     */
    public EDCAssetDto withContext(Map<String, String> context) {
        return new EDCAssetDto(
            this.id, this.assetId, this.jsonLDType, this.name, this.url,
            this.type, this.contentType, this.description, this.targetEDCId,
            this.dataAddress, this.properties, 
            context != null ? new HashMap<>(context) : null
        );
    }
    
    /**
     * Factory-Methode, um ein Asset-DTO aus einem vereinfachten Format zu erstellen.
     * 
     * @param simplified Die vereinfachte Darstellung des Assets
     * @param targetEDCId Die ID der Ziel-EDC-Instanz
     * @return Ein neues DTO, erstellt aus dem vereinfachten Format
     */
    public static EDCAssetDto fromSimplifiedFormat(Map<String, Object> simplified, Long targetEDCId) {
        if (simplified == null) {
            return null;
        }
        
        EDCAssetDto dto = new EDCAssetDto();
        
        // Extrahiere die wichtigsten Felder aus der Map
        String assetId = (String) simplified.get("assetId");
        if (assetId != null) {
            dto = dto.withAssetId(assetId);
        }
        
        String name = (String) simplified.get("name");
        if (name != null) {
            dto = dto.withName(name);
        }
        
        String description = (String) simplified.get("description");
        if (description != null) {
            dto = dto.withDescription(description);
        }
        
        String type = (String) simplified.get("type");
        if (type != null) {
            dto = dto.withType(type);
        }
        
        String contentType = (String) simplified.get("contentType");
        if (contentType != null) {
            dto = dto.withContentType(contentType);
        }
        
        String url = (String) simplified.get("url");
        if (url != null) {
            dto = dto.withUrl(url);
        }
        
        // Verarbeite die DataAddress, falls vorhanden
        Map<String, Object> dataAddressMap = (Map<String, Object>) simplified.get("dataAddress");
        if (dataAddressMap != null) {
            EDCDataAddressDto addr = new EDCDataAddressDto();
            
            String baseUrl = (String) dataAddressMap.get("baseUrl");
            if (baseUrl != null) {
                addr = addr.withBaseUrl(baseUrl);
            }
            
            Boolean proxyPath = (Boolean) dataAddressMap.get("proxyPath");
            if (proxyPath != null) {
                addr = addr.withProxyPath(proxyPath);
            }
            
            Boolean proxyQueryParams = (Boolean) dataAddressMap.get("proxyQueryParams");
            if (proxyQueryParams != null) {
                addr = addr.withProxyQueryParams(proxyQueryParams);
            }
            
            dto = dto.withDataAddress(addr);
        }
        
        dto = dto.withTargetEDCId(targetEDCId);
        
        return dto;
    }
}