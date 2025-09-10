package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entitätsklasse für EDC-Policies in der Datenbank.
 * Diese Klasse repräsentiert eine Policy im Eclipse Dataspace Connector (EDC) System
 * und speichert alle relevanten Informationen persistent in der Datenbank.
 * Eine Policy definiert Zugriffs- und Nutzungsbedingungen für Assets im EDC-System.
 * Die vollständige Policy-Definition wird als JSON-String gespeichert.
 */
@Setter
@Getter
@Entity
@Table(name = "edc_policy")
public class EDCPolicy extends UuidEntity {

    /**
     * Die Policy-ID als String, wie sie im EDC verwendet wird.
     * Muss eindeutig innerhalb des Systems sein.
     * Beispiel: "my-policy-id" oder "policy-for-asset-123"
     */
    @Column(nullable = false, unique = true)
    private String policyId;
    
    /**
     * Ein optionaler Anzeigename für die Policy.
     * Dieser wird im Frontend zur übersichtlicheren Darstellung verwendet.
     */
    @Column
    private String displayName;

    /**
     * Die vollständige Policy-Definition als JSON-String.
     * Speichert die komplette Policy-Struktur (context, permissions, usw.)
     * als serialisierten JSON-String für maximale Flexibilität.
     */
    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String policyJson;
    
    /**
     * Die zugehörige EDC-Instanz, zu der diese Policy gehört.
     * Eine Policy ist immer genau einer EDC-Instanz zugeordnet.
     */
    @ManyToOne
    @JoinColumn(name = "edc_instance_id")
    private EDCInstance edcInstance;

}
