package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;

/**
 * Data Transfer Object (DTO) für EDC-Asset-Properties.
 * Repräsentiert die Eigenschaften eines EDC-Assets in einer Form, die für die 
 * JSON-Serialisierung geeignet ist.
 * Implementiert als Record für mehr Effizienz und bessere Lesbarkeit.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EDCPropertyDto(
    /**
     * Die ID des DTOs, wird nicht in der JSON-Antwort enthalten sein.
     */
    @JsonIgnore
    Long id,
    
    /**
     * Die Map, die alle Properties des Assets enthält.
     * Diese wird bei der JSON-Serialisierung als Schlüssel-Wert-Paare auf der obersten Ebene dargestellt.
     */
    Map<String, Object> properties
) {
    /**
     * Konstanten für die standardmäßigen Property-Namen im EDC-Format.
     */
    public static final String PROP_NAME = "asset:prop:name";
    public static final String PROP_DESCRIPTION = "asset:prop:description";
    public static final String PROP_CONTENTTYPE = "asset:prop:contenttype";
    public static final String PROP_VERSION = "asset:prop:version";
    
    /**
     * Default-Konstruktor.
     */
    public EDCPropertyDto() {
        this(null, new HashMap<>());
    }
    
    /**
     * Konstruktor mit ID.
     * 
     * @param id Die ID des DTOs
     */
    public EDCPropertyDto(Long id) {
        this(id, new HashMap<>());
    }
    
    /**
     * Konstruktor mit einer Map von Properties.
     * 
     * @param properties Die Map von Properties
     */
    public EDCPropertyDto(Map<String, Object> properties) {
        this(null, properties);
    }
    
    /**
     * Canonical constructor with validation and defensive copying.
     */
    public EDCPropertyDto {
        // Defensive copy for mutable property
        if (properties != null) {
            properties = new HashMap<>(properties);
        } else {
            properties = new HashMap<>();
        }
    }
    
    /**
     * Gibt den Namen des Assets zurück.
     * 
     * @return Der Name des Assets oder null, wenn nicht gesetzt
     */
    public String getName() {
        return (String) properties.get(PROP_NAME);
    }
    
    /**
     * Erstellt ein neues DTO mit dem angegebenen Namen.
     * 
     * @param name Der zu setzende Name
     * @return Ein neues DTO mit dem gesetzten Namen
     */
    public EDCPropertyDto withName(String name) {
        Map<String, Object> newProps = new HashMap<>(this.properties);
        if (name != null) {
            newProps.put(PROP_NAME, name);
        }
        return new EDCPropertyDto(this.id, newProps);
    }
    
    /**
     * Gibt die Version des Assets zurück.
     * 
     * @return Die Version des Assets oder null, wenn nicht gesetzt
     */
    public String getVersion() {
        return (String) properties.get(PROP_VERSION);
    }
    
    /**
     * Erstellt ein neues DTO mit der angegebenen Version.
     * 
     * @param version Die zu setzende Version
     * @return Ein neues DTO mit der gesetzten Version
     */
    public EDCPropertyDto withVersion(String version) {
        Map<String, Object> newProps = new HashMap<>(this.properties);
        if (version != null) {
            newProps.put(PROP_VERSION, version);
        }
        return new EDCPropertyDto(this.id, newProps);
    }
    
    /**
     * Gibt den Content-Type des Assets zurück.
     * 
     * @return Der Content-Type des Assets oder null, wenn nicht gesetzt
     */
    public String getContentType() {
        return (String) properties.get(PROP_CONTENTTYPE);
    }
    
    /**
     * Erstellt ein neues DTO mit dem angegebenen Content-Type.
     * 
     * @param contentType Der zu setzende Content-Type
     * @return Ein neues DTO mit dem gesetzten Content-Type
     */
    public EDCPropertyDto withContentType(String contentType) {
        Map<String, Object> newProps = new HashMap<>(this.properties);
        if (contentType != null) {
            newProps.put(PROP_CONTENTTYPE, contentType);
        }
        return new EDCPropertyDto(this.id, newProps);
    }
    
    /**
     * Gibt die Beschreibung des Assets zurück.
     * 
     * @return Die Beschreibung des Assets oder null, wenn nicht gesetzt
     */
    public String getDescription() {
        return (String) properties.get(PROP_DESCRIPTION);
    }
    
    /**
     * Erstellt ein neues DTO mit der angegebenen Beschreibung.
     * 
     * @param description Die zu setzende Beschreibung
     * @return Ein neues DTO mit der gesetzten Beschreibung
     */
    public EDCPropertyDto withDescription(String description) {
        Map<String, Object> newProps = new HashMap<>(this.properties);
        if (description != null) {
            newProps.put(PROP_DESCRIPTION, description);
        }
        return new EDCPropertyDto(this.id, newProps);
    }
    
    /**
     * Gibt alle Properties des Assets zurück.
     * Diese werden bei der JSON-Serialisierung als Top-Level-Eigenschaften eingefügt.
     * 
     * @return Eine Map mit allen Properties
     */
    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return properties;
    }
    
    /**
     * Erstellt ein neues DTO mit der hinzugefügten Property.
     * 
     * @param key Der Schlüssel der Property
     * @param value Der Wert der Property
     * @return Ein neues DTO mit der hinzugefügten Property
     */
    public EDCPropertyDto withProperty(String key, Object value) {
        Map<String, Object> newProps = new HashMap<>(this.properties);
        if (key != null && value != null) {
            newProps.put(key, value);
        }
        return new EDCPropertyDto(this.id, newProps);
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
}
