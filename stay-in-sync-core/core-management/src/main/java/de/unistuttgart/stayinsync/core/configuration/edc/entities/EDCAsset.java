package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemEndpoint;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity-Klasse für EDC-Assets (Eclipse Dataspace Connector).
 * Repräsentiert ein Asset, das über den EDC bereitgestellt werden kann.
 * Diese Entität ist das Backend-Pendant zu dem Asset-Format, das im Frontend verwendet wird.
 */
@Entity
@Table(name = "edc_asset", uniqueConstraints = {
    @UniqueConstraint(columnNames = "asset_id")
})
public class EDCAsset extends PanacheEntity {

    /**
     * Die eindeutige Business-ID des Assets im EDC-System.
     * Unterscheidet sich von der technischen ID dieser Entity.
     */
    @NotBlank
    @Column(name = "asset_id", nullable = false)
    public String assetId;

    /**
     * Die URL, unter der das Asset erreichbar ist.
     * Wird auch in der DataAddress verwendet.
     */
    @Column(name = "url", nullable = false)
    public String url;

    /**
     * Der Typ des Assets, typischerweise "HttpData" für REST-API-basierte Assets.
     */
    @Column(name = "type", nullable = false)
    public String type;

    /**
     * Der Content-Type des Assets, z.B. "application/json".
     */
    @Column(name = "content_type", nullable = false)
    public String contentType;

    /**
     * Eine optionale Beschreibung des Assets.
     */
    @Column(name = "description", length = 1024)
    public String description;

    /**
     * Die Ziel-EDC-Instanz, zu der dieses Asset gehört.
     * Jedes Asset muss genau einer EDC-Instanz zugeordnet sein.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_edc_id", nullable = false)
    public EDCInstance targetEDC;

    /**
     * Die Daten-Adresse des Assets, die Informationen zum Zugriff enthält.
     * Wird beim Löschen des Assets automatisch mit gelöscht (CascadeType.ALL).
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "data_address_id", nullable = false)
    public EDCDataAddress dataAddress;

    /**
     * Eigenschaften des Assets.
     * Werden beim Löschen des Assets automatisch mit gelöscht (CascadeType.ALL).
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "properties_id")
    public EDCProperty properties;

    /**
     * Optionale Zuordnung zu einem Ziel-System-Endpunkt.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_system_endpoint_id", nullable = true)
    public TargetSystemEndpoint targetSystemEndpoint;
    
    /**
     * Die Zugriffsrichtlinie (Access Policy), die diesem Asset zugeordnet ist.
     * Definiert, wer auf das Asset zugreifen darf.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_policy_id", nullable = true)
    public EDCPolicy accessPolicy;
    
    /**
     * Die Vertragsrichtlinie (Contract Policy), die diesem Asset zugeordnet ist.
     * Definiert die Bedingungen, unter denen auf das Asset zugegriffen werden darf.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_policy_id", nullable = true)
    public EDCPolicy contractPolicy;

    /**
     * Default-Konstruktor für JPA.
     */
    public EDCAsset() {
        // Standardwerte setzen
        this.type = "HttpData";
        this.contentType = "application/json";
    }

    /**
     * Konstruktor mit Basis-Eigenschaften.
     * 
     * @param assetId Die Business-ID des Assets
     * @param url Die URL, unter der das Asset erreichbar ist
     * @param targetEDC Die EDC-Instanz, zu der das Asset gehört
     */
    public EDCAsset(String assetId, String url, EDCInstance targetEDC) {
        this();
        this.assetId = assetId;
        this.url = url;
        this.targetEDC = targetEDC;
        
        // Erstelle Standard-DataAddress
        this.dataAddress = new EDCDataAddress(url);
        
        // Erstelle Standard-Properties
        this.properties = new EDCProperty();
    }

    /**
     * Vollständiger Konstruktor.
     * 
     * @param assetId Die Business-ID des Assets
     * @param url Die URL, unter der das Asset erreichbar ist
     * @param type Der Typ des Assets
     * @param contentType Der Content-Type des Assets
     * @param description Die Beschreibung des Assets
     * @param targetEDC Die EDC-Instanz, zu der das Asset gehört
     * @param dataAddress Die Daten-Adresse des Assets
     * @param properties Die Eigenschaften des Assets
     */
    public EDCAsset(String assetId, String url, String type, String contentType, String description,
                  EDCInstance targetEDC, EDCDataAddress dataAddress, EDCProperty properties) {
        this.assetId = assetId;
        this.url = url;
        this.type = type != null ? type : "HttpData";
        this.contentType = contentType != null ? contentType : "application/json";
        this.description = description;
        this.targetEDC = targetEDC;
        this.dataAddress = dataAddress;
        this.properties = properties;
    }
    
    /**
     * Findet ein EDCAsset anhand seiner assetId (Business-ID).
     * 
     * @param assetId Die Business-ID des Assets
     * @return Das gefundene Asset oder null, wenn kein Asset mit dieser ID existiert
     */
    public static EDCAsset findByAssetId(String assetId) {
        return find("assetId", assetId).firstResult();
    }

    /**
     * Spezielle Methode für Type, um Standardwert "HttpData" zu gewährleisten.
     */
    public void setType(String type) {
        this.type = type != null ? type : "HttpData";
    }

    /**
     * Spezielle Methode für ContentType, um Standardwert "application/json" zu gewährleisten.
     */
    public void setContentType(String contentType) {
        this.contentType = contentType != null ? contentType : "application/json";
    }
    
    /**
     * Erstellt oder aktualisiert die Properties, falls nötig.
     * Diese Methode stellt sicher, dass das Asset über gültige Properties verfügt.
     */
    public void ensureProperties() {
        if (properties == null) {
            properties = new EDCProperty();
        }
        
        // Content-Type von Asset zu Properties übertragen
        if (contentType != null) {
            // Für Felder mit spezieller Logik im Setter weiterhin den Setter verwenden
            properties.setContentType(contentType);
        }
        
        // Beschreibung von Asset zu Properties übertragen
        if (description != null) {
            // Direkter Feldzugriff nach Panache-Stil
            properties.description = description;
        }
        
        // Asset-ID zu Properties hinzufügen
        properties.name = assetId;
    }
    
    /**
     * Erstellt oder aktualisiert die DataAddress, falls nötig.
     * Diese Methode stellt sicher, dass das Asset über eine gültige DataAddress verfügt.
     */
    public void ensureDataAddress() {
        if (dataAddress == null) {
            dataAddress = new EDCDataAddress();
        }
        
        // URL von Asset zu DataAddress übertragen
        if (url != null) {
            // Bei Panache können wir direkt auf die Felder zugreifen
            dataAddress.baseUrl = url;
        }
        
        // Typ von Asset zu DataAddress übertragen
        if (type != null) {
            // Bei komplexen Settern (mit Logik) weiterhin die Setter verwenden
            dataAddress.setType(type);
        }
    }

    /**
     * Wird vor dem Speichern aufgerufen, um sicherzustellen, dass alle erforderlichen
     * Beziehungen korrekt gesetzt sind.
     */
    @PrePersist
    @PreUpdate
    public void prePersist() {
        ensureProperties();
        ensureDataAddress();
    }
}
