package de.unistuttgart.stayinsync.core.configuration.edc.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object (DTO) für EDC-Asset-Properties.
 * Repräsentiert die Eigenschaften eines EDC-Assets in einer Form, die für die 
 * JSON-Serialisierung geeignet ist.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EDCPropertyDto {
    /**
     * Konstanten für die standardmäßigen Property-Namen im EDC-Format.
     */
    public static final String PROP_NAME = "asset:prop:name";
    public static final String PROP_DESCRIPTION = "asset:prop:description";
    public static final String PROP_CONTENTTYPE = "asset:prop:contenttype";
    public static final String PROP_VERSION = "asset:prop:version";
    
    /**
     * Die ID des DTOs, wird nicht in der JSON-Antwort enthalten sein.
     */
    @JsonIgnore
    private UUID id;
    
    /**
     * Die Map, die alle Properties des Assets enthält.
     * Diese wird bei der JSON-Serialisierung als Schlüssel-Wert-Paare auf der obersten Ebene dargestellt.
     */
    private Map<String, Object> properties = new HashMap<>();
    
    /**
     * Default-Konstruktor.
     */
    public EDCPropertyDto() {
        // Leere Map initialisieren
    }
    
    /**
     * Konstruktor mit ID.
     * 
     * @param id Die ID des DTOs
     */
    public EDCPropertyDto(UUID id) {
        this.id = id;
    }
    
    /**
     * Konstruktor mit einer Map von Properties.
     * 
     * @param properties Die Map von Properties
     */
    public EDCPropertyDto(Map<String, Object> properties) {
        if (properties != null) {
            this.properties = new HashMap<>(properties);
        }
    }
    
    /**
     * Vollständiger Konstruktor.
     * 
     * @param id Die ID des DTOs
     * @param properties Die Map von Properties
     */
    public EDCPropertyDto(UUID id, Map<String, Object> properties) {
        this.id = id;
        if (properties != null) {
            this.properties = new HashMap<>(properties);
        }
    }
    
    /**
     * Gibt die ID des DTOs zurück.
     * 
     * @return Die UUID des DTOs
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Setzt die ID des DTOs.
     * 
     * @param id Die zu setzende UUID
     * @return Das DTO selbst für Method Chaining
     */
    public EDCPropertyDto setId(UUID id) {
        this.id = id;
        return this;
    }
    
    /**
     * Gibt den Namen des Assets zurück.
     * 
     * @return Der Name des Assets
     */
    public String getName() {
        return (String) properties.get(PROP_NAME);
    }
    
    /**
     * Setzt den Namen des Assets.
     * 
     * @param name Der zu setzende Name
     * @return Das DTO selbst für Method Chaining
     */
    public EDCPropertyDto setName(String name) {
        if (name != null) {
            properties.put(PROP_NAME, name);
        }
        return this;
    }
    
    /**
     * Gibt die Version des Assets zurück.
     * 
     * @return Die Version des Assets
     */
    public String getVersion() {
        return (String) properties.get(PROP_VERSION);
    }
    
    /**
     * Setzt die Version des Assets.
     * 
     * @param version Die zu setzende Version
     * @return Das DTO selbst für Method Chaining
     */
    public EDCPropertyDto setVersion(String version) {
        if (version != null) {
            properties.put(PROP_VERSION, version);
        }
        return this;
    }
    
    /**
     * Gibt den Content-Type des Assets zurück.
     * 
     * @return Der Content-Type des Assets
     */
    public String getContentType() {
        return (String) properties.get(PROP_CONTENTTYPE);
    }
    
    /**
     * Setzt den Content-Type des Assets.
     * 
     * @param contentType Der zu setzende Content-Type
     * @return Das DTO selbst für Method Chaining
     */
    public EDCPropertyDto setContentType(String contentType) {
        if (contentType != null) {
            properties.put(PROP_CONTENTTYPE, contentType);
        }
        return this;
    }
    
    /**
     * Gibt die Beschreibung des Assets zurück.
     * 
     * @return Die Beschreibung des Assets
     */
    public String getDescription() {
        return (String) properties.get(PROP_DESCRIPTION);
    }
    
    /**
     * Setzt die Beschreibung des Assets.
     * 
     * @param description Die zu setzende Beschreibung
     * @return Das DTO selbst für Method Chaining
     */
    public EDCPropertyDto setDescription(String description) {
        if (description != null) {
            properties.put(PROP_DESCRIPTION, description);
        }
        return this;
    }
    
    /**
     * Gibt alle Properties des Assets zurück.
     * Diese werden bei der JSON-Serialisierung als Top-Level-Eigenschaften eingefügt.
     * 
     * @return Eine Map mit allen Properties
     */
    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    /**
     * Hilfsmethode zum Hinzufügen einer Property.
     * 
     * @param key Der Schlüssel der Property
     * @param value Der Wert der Property
     * @return Das DTO selbst für Method Chaining
     */
    public EDCPropertyDto addProperty(String key, Object value) {
        if (key != null && value != null) {
            properties.put(key, value);
        }
        return this;
    }
    
    /**
     * Setzt eine Property.
     * 
     * @param name Der Name der Property
     * @param value Der Wert der Property
     */
    @JsonAnySetter
    public void setProperty(String name, Object value) {
        if (name != null && value != null) {
            properties.put(name, value);
        }
    }
    
    /**
     * Setzt alle Properties.
     * 
     * @param properties Eine Map mit allen zu setzenden Properties
     * @return Das DTO selbst für Method Chaining
     */
    public EDCPropertyDto setProperties(Map<String, Object> properties) {
        this.properties = properties != null ? new HashMap<>(properties) : new HashMap<>();
        return this;
    }
    
    /**
     * Gibt eine String-Repräsentation des DTOs zurück.
     * 
     * @return Eine lesbare Darstellung des DTOs
     */
    @Override
    public String toString() {
        return "EDCPropertyDto{" +
                "id=" + id +
                ", properties=" + properties +
                '}';
    }
    
    /**
     * Hilfsmethode, um die Map für die Kompatibilität mit der alten Implementierung zurückzugeben.
     * 
     * @return Die Map mit allen Properties
     */
    public Map<String, Object> getAdditionalProperties() {
        return properties;
    }
}
