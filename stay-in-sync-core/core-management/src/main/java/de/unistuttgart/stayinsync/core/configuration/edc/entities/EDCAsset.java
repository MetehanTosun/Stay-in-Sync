package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemEndpoint;
import de.unistuttgart.stayinsync.core.model.UuidEntity;
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
public class EDCAsset extends UuidEntity {

    /**
     * Die eindeutige Business-ID des Assets im EDC-System.
     * Unterscheidet sich von der technischen UUID dieser Entity.
     */
    @NotBlank
    @Column(name = "asset_id", nullable = false)
    private String assetId;

    /**
     * Die URL, unter der das Asset erreichbar ist.
     * Wird auch in der DataAddress verwendet.
     */
    @Column(name = "url", nullable = false)
    private String url;

    /**
     * Der Typ des Assets, typischerweise "HttpData" für REST-API-basierte Assets.
     */
    @Column(name = "type", nullable = false)
    private String type;

    /**
     * Der Content-Type des Assets, z.B. "application/json".
     */
    @Column(name = "content_type", nullable = false)
    private String contentType;

    /**
     * Eine optionale Beschreibung des Assets.
     */
    @Column(name = "description", length = 1024)
    private String description;

    /**
     * Die Ziel-EDC-Instanz, zu der dieses Asset gehört.
     * Jedes Asset muss genau einer EDC-Instanz zugeordnet sein.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_edc_id", columnDefinition = "CHAR(36)", nullable = false)
    private EDCInstance targetEDC;

    /**
     * Die Daten-Adresse des Assets, die Informationen zum Zugriff enthält.
     * Wird beim Löschen des Assets automatisch mit gelöscht (CascadeType.ALL).
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "data_address_id", columnDefinition = "CHAR(36)", nullable = false)
    private EDCDataAddress dataAddress;

    /**
     * Eigenschaften des Assets.
     * Werden beim Löschen des Assets automatisch mit gelöscht (CascadeType.ALL).
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "properties_id", columnDefinition = "CHAR(36)")
    private EDCProperty properties;

    /**
     * Optionale Zuordnung zu einem Ziel-System-Endpunkt.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_system_endpoint_id", nullable = true)
    private TargetSystemEndpoint targetSystemEndpoint;
    
    /**
     * Die Zugriffsrichtlinie (Access Policy), die diesem Asset zugeordnet ist.
     * Definiert, wer auf das Asset zugreifen darf.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "access_policy_id", columnDefinition = "CHAR(36)", nullable = true)
    private EDCPolicy accessPolicy;
    
    /**
     * Die Vertragsrichtlinie (Contract Policy), die diesem Asset zugeordnet ist.
     * Definiert die Bedingungen, unter denen auf das Asset zugegriffen werden darf.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_policy_id", columnDefinition = "CHAR(36)", nullable = true)
    private EDCPolicy contractPolicy;

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
     * Getter für assetId.
     */
    public String getAssetId() {
        return assetId;
    }

    /**
     * Setter für assetId.
     */
    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    /**
     * Getter für url.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Setter für url.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Getter für type.
     */
    public String getType() {
        return type;
    }

    /**
     * Setter für type.
     */
    public void setType(String type) {
        this.type = type != null ? type : "HttpData";
    }

    /**
     * Getter für contentType.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Setter für contentType.
     */
    public void setContentType(String contentType) {
        this.contentType = contentType != null ? contentType : "application/json";
    }

    /**
     * Getter für description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Setter für description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Getter für targetEDC.
     */
    public EDCInstance getTargetEDC() {
        return targetEDC;
    }

    /**
     * Setter für targetEDC.
     */
    public void setTargetEDC(EDCInstance targetEDC) {
        this.targetEDC = targetEDC;
    }

    /**
     * Getter für dataAddress.
     */
    public EDCDataAddress getDataAddress() {
        return dataAddress;
    }

    /**
     * Setter für dataAddress.
     */
    public void setDataAddress(EDCDataAddress dataAddress) {
        this.dataAddress = dataAddress;
    }

    /**
     * Getter für properties.
     */
    public EDCProperty getProperties() {
        return properties;
    }

    /**
     * Setter für properties.
     */
    public void setProperties(EDCProperty properties) {
        this.properties = properties;
    }

    /**
     * Getter für targetSystemEndpoint.
     */
    public TargetSystemEndpoint getTargetSystemEndpoint() {
        return targetSystemEndpoint;
    }

    /**
     * Setter für targetSystemEndpoint.
     */
    public void setTargetSystemEndpoint(TargetSystemEndpoint targetSystemEndpoint) {
        this.targetSystemEndpoint = targetSystemEndpoint;
    }
    
    /**
     * Getter für accessPolicy.
     */
    public EDCPolicy getAccessPolicy() {
        return accessPolicy;
    }
    
    /**
     * Setter für accessPolicy.
     */
    public void setAccessPolicy(EDCPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }
    
    /**
     * Getter für contractPolicy.
     */
    public EDCPolicy getContractPolicy() {
        return contractPolicy;
    }
    
    /**
     * Setter für contractPolicy.
     */
    public void setContractPolicy(EDCPolicy contractPolicy) {
        this.contractPolicy = contractPolicy;
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
            properties.setContentType(contentType);
        }
        
        // Beschreibung von Asset zu Properties übertragen
        if (description != null) {
            properties.setDescription(description);
        }
        
        // Asset-ID zu Properties hinzufügen
        properties.setName(assetId);
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
            dataAddress.setBaseUrl(url);
        }
        
        // Typ von Asset zu DataAddress übertragen
        if (type != null) {
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
