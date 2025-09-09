package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TargetSystemEndpoint;
import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * Entity-Klasse für EDC-Assets (Eclipse Dataspace Connector).
 * Repräsentiert ein Asset, das über den EDC bereitgestellt werden kann.
 * Diese Entität ist das Backend-Pendant zu dem Asset-Format, das im Frontend verwendet wird.
 */
@Entity
@Table(name = "edc_asset")
public class EDCAsset extends UuidEntity {

    /**
     * Die eindeutige Business-ID des Assets im EDC-System.
     * Unterscheidet sich von der technischen UUID dieser Entity.
     */
    @Getter
    @Setter
    @NotBlank
    @Column(nullable = false)
    private String assetId;

    /**
     * Die URL, unter der das Asset erreichbar ist.
     */
    @Getter
    @Setter
    @NotBlank
    @Column(nullable = false)
    private String url;

    /**
     * Der Typ des Assets, typischerweise "HttpData" für REST-API-basierte Assets.
     */
    @Getter
    @Setter
    @NotBlank
    @Column(nullable = false)
    private String type;

    /**
     * Der Content-Type des Assets, z.B. "application/json".
     */
    @Getter
    @Setter
    @NotBlank
    @Column(nullable = false)
    private String contentType;

    /**
     * Eine optionale Beschreibung des Assets.
     */
    @Getter
    @Setter
    @Column(length = 1024)
    private String description;

    /**
     * Die Ziel-EDC-Instanz, zu der dieses Asset gehört.
     * Jedes Asset muss genau einer EDC-Instanz zugeordnet sein.
     */
    @Getter
    @Setter
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_edc_id", columnDefinition = "CHAR(36)", nullable = false)
    private EDCInstance targetEDC;

    /**
     * Die Daten-Adresse des Assets, die Informationen zum Zugriff enthält.
     * Wird beim Löschen des Assets automatisch mit gelöscht (CascadeType.ALL).
     */
    @Getter
    @Setter
    @ManyToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "data_address_id", columnDefinition = "CHAR(36)", nullable = false)
    private EDCDataAddress dataAddress;

    /**
     * Optionale zusätzliche Eigenschaften des Assets.
     * Werden beim Löschen des Assets automatisch mit gelöscht (CascadeType.ALL).
     */
    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "properties_id", columnDefinition = "CHAR(36)")
    private EDCProperty properties;

    /**
     * Optionale Zuordnung zu einem Ziel-System-Endpunkt.
     */
    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_system_endpoint_id", nullable = true)
    private TargetSystemEndpoint targetSystemEndpoint;
    
    /**
     * Findet ein EDCAsset anhand seiner assetId (Business-ID).
     * 
     * @param assetId Die Business-ID des Assets
     * @return Das gefundene Asset oder null, wenn kein Asset mit dieser ID existiert
     */
    public static EDCAsset findByAssetId(String assetId) {
        return find("assetId", assetId).firstResult();
    }
}
