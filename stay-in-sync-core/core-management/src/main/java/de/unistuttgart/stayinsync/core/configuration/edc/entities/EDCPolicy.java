package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import de.unistuttgart.stayinsync.core.model.UuidEntity;
import jakarta.persistence.*;

/**
 * Entitätsklasse für EDC-Policies in der Datenbank.
 * 
 * Diese Klasse repräsentiert eine Policy im Eclipse Dataspace Connector (EDC) System
 * und speichert alle relevanten Informationen persistent in der Datenbank.
 * 
 * Eine Policy definiert Zugriffs- und Nutzungsbedingungen für Assets im EDC-System.
 * Die vollständige Policy-Definition wird als JSON-String gespeichert.
 */
@Entity
@Table(name = "edc_policy")
public class EDCPolicy extends UuidEntity {

    /**
     * Sucht eine Policy anhand ihrer policyId (String-ID).
     * 
     * @param policyId Die Policy-ID als String
     * @return Die gefundene Policy oder null, wenn keine Policy mit dieser ID existiert
     */
    public static EDCPolicy findByPolicyId(String policyId) {
        if (policyId == null || policyId.isEmpty()) {
            return null;
        }
        return find("policyId", policyId).firstResult();
    }
    
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
    
    /**
     * Gibt die Policy-ID zurück.
     * 
     * @return die Policy-ID als String
     */
    public String getPolicyId() {
        return policyId;
    }
    
    /**
     * Setzt die Policy-ID.
     * 
     * @param policyId die zu setzende Policy-ID
     */
    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }
    
    /**
     * Gibt den Anzeigenamen der Policy zurück.
     * Falls kein Anzeigename gesetzt ist, wird die Policy-ID zurückgegeben.
     * 
     * @return der Anzeigename oder die Policy-ID
     */
    public String getDisplayName() {
        return displayName != null ? displayName : policyId;
    }
    
    /**
     * Setzt den Anzeigenamen der Policy.
     * 
     * @param displayName der zu setzende Anzeigename
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Gibt den JSON-String der Policy-Definition zurück.
     * 
     * @return die Policy-Definition als JSON-String
     */
    public String getPolicyJson() {
        return policyJson;
    }
    
    /**
     * Setzt den JSON-String der Policy-Definition.
     * 
     * @param policyJson der zu setzende JSON-String
     */
    public void setPolicyJson(String policyJson) {
        this.policyJson = policyJson;
    }
    
    /**
     * Gibt die zugehörige EDC-Instanz zurück.
     * 
     * @return die EDC-Instanz, zu der diese Policy gehört
     */
    public EDCInstance getEdcInstance() {
        return edcInstance;
    }
    
    /**
     * Setzt die zugehörige EDC-Instanz.
     * 
     * @param edcInstance die zu setzende EDC-Instanz
     */
    public void setEdcInstance(EDCInstance edcInstance) {
        this.edcInstance = edcInstance;
    }
}
