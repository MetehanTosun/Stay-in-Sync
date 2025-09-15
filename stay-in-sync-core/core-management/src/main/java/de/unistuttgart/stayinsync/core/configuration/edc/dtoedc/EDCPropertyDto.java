package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object (DTO) für EDC-Asset-Properties.
 * Repräsentiert die Eigenschaften eines EDC-Assets in einer Form, die für die 
 * JSON-Serialisierung geeignet ist.
 */
public class EDCPropertyDto {
    @JsonIgnore
    private UUID id;

    @JsonProperty("asset:prop:name")
    private String name;
    
    @JsonProperty("asset:prop:version")
    private String version;
    
    @JsonProperty("asset:prop:contenttype")
    private String contentType;
    
    @JsonIgnore
    private String description;
    
    private Map<String, String> additionalProperties = new HashMap<>();

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
        return name;
    }

    /**
     * Setzt den Namen des Assets.
     * 
     * @param name Der zu setzende Name
     * @return Das DTO selbst für Method Chaining
     */
    public EDCPropertyDto setName(String name) {
        this.name = name;
        // Auch als zusätzliche Eigenschaft speichern
        if (name != null) {
            additionalProperties.put("asset:prop:name", name);
        }
        return this;
    }

    /**
     * Gibt die Version des Assets zurück.
     * 
     * @return Die Version des Assets
     */
    public String getVersion() {
        return version;
    }

    /**
     * Setzt die Version des Assets.
     * 
     * @param version Die zu setzende Version
     * @return Das DTO selbst für Method Chaining
     */
    public EDCPropertyDto setVersion(String version) {
        this.version = version;
        // Auch als zusätzliche Eigenschaft speichern
        if (version != null) {
            additionalProperties.put("asset:prop:version", version);
        }
        return this;
    }

    /**
     * Gibt den Content-Type des Assets zurück.
     * 
     * @return Der Content-Type des Assets
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Setzt den Content-Type des Assets.
     * 
     * @param contentType Der zu setzende Content-Type
     * @return Das DTO selbst für Method Chaining
     */
    public EDCPropertyDto setContentType(String contentType) {
        this.contentType = contentType;
        // Auch als zusätzliche Eigenschaft speichern
        if (contentType != null) {
            additionalProperties.put("asset:prop:contenttype", contentType);
        }
        return this;
    }

    /**
     * Gibt die Beschreibung des Assets zurück.
     * 
     * @return Die Beschreibung des Assets
     */
    public String getDescription() {
        return description;
    }

    /**
     * Setzt die Beschreibung des Assets.
     * 
     * @param description Die zu setzende Beschreibung
     * @return Das DTO selbst für Method Chaining
     */
    public EDCPropertyDto setDescription(String description) {
        this.description = description;
        // Auch als zusätzliche Eigenschaft speichern
        if (description != null) {
            additionalProperties.put("asset:prop:description", description);
        }
        return this;
    }
    
    /**
     * Gibt alle zusätzlichen Eigenschaften des Assets zurück.
     * Diese werden bei der JSON-Serialisierung als Top-Level-Eigenschaften eingefügt.
     * 
     * @return Eine Map mit allen zusätzlichen Eigenschaften
     */
    @JsonAnyGetter
    public Map<String, String> getAdditionalProperties() {
        return additionalProperties;
    }
    
    /**
     * Setzt eine zusätzliche Eigenschaft.
     * Wenn es sich um eine bekannte Eigenschaft handelt, wird auch das entsprechende Feld gesetzt.
     * 
     * @param name Der Name der Eigenschaft
     * @param value Der Wert der Eigenschaft
     */
    @JsonAnySetter
    public void setAdditionalProperty(String name, String value) {
        additionalProperties.put(name, value);
        
        // Bekannte Eigenschaften in die entsprechenden Felder setzen
        switch (name) {
            case "asset:prop:description" -> this.description = value;
            case "asset:prop:name" -> this.name = value;
            case "asset:prop:version" -> this.version = value;
            case "asset:prop:contenttype" -> this.contentType = value;
        }
    }
    
    /**
     * Setzt alle zusätzlichen Eigenschaften.
     * Extrahiert bekannte Eigenschaften in die entsprechenden Felder.
     * 
     * @param properties Eine Map mit allen zu setzenden Eigenschaften
     */
    public void setAdditionalProperties(Map<String, String> properties) {
        this.additionalProperties = properties != null ? properties : new HashMap<>();
        
        // Bekannte Eigenschaften extrahieren
        if (properties != null) {
            if (properties.containsKey("asset:prop:description")) {
                this.description = properties.get("asset:prop:description");
            }
            if (properties.containsKey("asset:prop:name")) {
                this.name = properties.get("asset:prop:name");
            }
            if (properties.containsKey("asset:prop:version")) {
                this.version = properties.get("asset:prop:version");
            }
            if (properties.containsKey("asset:prop:contenttype")) {
                this.contentType = properties.get("asset:prop:contenttype");
            }
        }
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
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", contentType='" + contentType + '\'' +
                ", description='" + description + '\'' +
                ", additionalProperties=" + additionalProperties +
                '}';
    }
}
