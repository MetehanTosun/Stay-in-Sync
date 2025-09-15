package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Entity-Klasse für EDC-Properties.
 * Repräsentiert die Eigenschaften eines Assets im EDC-System.
 */
@Entity
@Table(name = "edc_property")
public class EDCProperty extends UuidEntity {

    /**
     * Der Name des Assets.
     */
    @Column(name = "name", length = 255)
    private String name;

    /**
     * Die Beschreibung des Assets.
     */
    @Column(name = "description", length = 1024)
    private String description;

    /**
     * Der Content-Type des Assets, standardmäßig "application/json".
     */
    @Column(name = "content_type", length = 255)
    private String contentType;

    /**
     * Die Version des Assets.
     */
    @Column(name = "version", length = 50)
    private String version;

    /**
     * Default-Konstruktor für JPA.
     */
    public EDCProperty() {
        // Standard Content-Type setzen
        this.contentType = "application/json";
        this.version = "1.0.0";
    }

    /**
     * Convenience-Konstruktor mit Beschreibung.
     * 
     * @param description Die Beschreibung des Assets
     */
    public EDCProperty(String description) {
        this();
        this.description = description;
    }

    /**
     * Vollständiger Konstruktor.
     * 
     * @param name Der Name des Assets
     * @param description Die Beschreibung des Assets
     * @param contentType Der Content-Type des Assets
     * @param version Die Version des Assets
     */
    public EDCProperty(String name, String description, String contentType, String version) {
        this.name = name;
        this.description = description;
        this.contentType = contentType != null ? contentType : "application/json";
        this.version = version != null ? version : "1.0.0";
    }

    /**
     * Getter für den Namen.
     */
    public String getName() {
        return name;
    }

    /**
     * Setter für den Namen.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter für die Beschreibung.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Setter für die Beschreibung.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Getter für den Content-Type.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Setter für den Content-Type.
     */
    public void setContentType(String contentType) {
        this.contentType = contentType != null ? contentType : "application/json";
    }

    /**
     * Getter für die Version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Setter für die Version.
     */
    public void setVersion(String version) {
        this.version = version != null ? version : "1.0.0";
    }
}
