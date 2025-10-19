package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Entity-Klasse für EDC-Properties.
 * Repräsentiert die Eigenschaften eines Assets im EDC-System.
 */
@Entity
@Table(name = "edc_property")
public class Property extends PanacheEntity {

    /**
     * Der Name des Assets.
     */
    @Column(name = "name", length = 255)
    public String name;

    /**
     * Die Beschreibung des Assets.
     */
    @Column(name = "description", length = 1024)
    public String description;

    /**
     * Der Content-Type des Assets, standardmäßig "application/json".
     */
    @Column(name = "content_type", length = 255)
    public String contentType;

    /**
     * Die Version des Assets.
     */
    @Column(name = "version", length = 50)
    public String version;

    /**
     * Default-Konstruktor für JPA.
     */
    public Property() {
        // Standard Content-Type setzen
        this.contentType = "application/json";
        this.version = "1.0.0";
    }

    /**
     * Convenience-Konstruktor mit Beschreibung.
     * 
     * @param description Die Beschreibung des Assets
     */
    public Property(String description) {
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
    public Property(String name, String description, String contentType, String version) {
        this.name = name;
        this.description = description;
        this.contentType = contentType != null ? contentType : "application/json";
        this.version = version != null ? version : "1.0.0";
    }

    /**
     * Spezielle Methode für Content-Type, um Standardwert "application/json" zu gewährleisten.
     */
    public void setContentType(String contentType) {
        this.contentType = contentType != null ? contentType : "application/json";
    }

    /**
     * Spezielle Methode für Version, um Standardwert "1.0.0" zu gewährleisten.
     */
    public void setVersion(String version) {
        this.version = version != null ? version : "1.0.0";
    }
}
