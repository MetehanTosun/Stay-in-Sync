package de.unistuttgart.stayinsync.core.configuration.edc.entities;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

/**
 * Entitätsklasse für EDC-Policies in der Datenbank.
 * Diese Klasse repräsentiert eine Policy im Eclipse Dataspace Connector (EDC) System
 * und speichert alle relevanten Informationen persistent in der Datenbank.
 * Eine Policy definiert Zugriffs- und Nutzungsbedingungen für Assets im EDC-System.
 * Die vollständige Policy-Definition wird als JSON-String gespeichert.
 */
@NoArgsConstructor
@Entity
@Table(name = "edc_policy")
public class Policy extends PanacheEntity {

    /**
     * Die Policy-ID als String, wie sie im EDC verwendet wird.
     * Muss eindeutig innerhalb des Systems sein.
     * Beispiel: "my-policy-id" oder "policy-for-asset-123"
     */
    @Column(nullable = false, unique = true)
    public String policyId;
    
    /**
     * Ein optionaler Anzeigename für die Policy.
     * Dieser wird im Frontend zur übersichtlicheren Darstellung verwendet.
     */
    @Column
    public String displayName;

    /**
     * Die vollständige Policy-Definition als JSON-String.
     * Speichert die komplette Policy-Struktur (context, permissions, usw.)
     * als serialisierten JSON-String für maximale Flexibilität.
     */
    @Lob
    @Column(columnDefinition = "LONGTEXT", nullable = false)
    public String policyJson;
    
    /**
     * Die zugehörige EDC-Instanz, zu der diese Policy gehört.
     * Eine Policy ist immer genau einer EDC-Instanz zugeordnet.
     */
    @ManyToOne
    @JoinColumn(name = "edc_instance_id")
    public EdcInstance edcInstance;

    /**
     * Finder-Methode zum Abrufen einer Policy anhand ihrer policyId.
     * 
     * @param policyId Die zu suchende Policy-ID
     * @return Die gefundene EDC-Policy oder null, wenn keine gefunden wurde
     */
    public static Policy findByPolicyId(String policyId) {
        if (policyId == null || policyId.isEmpty()) {
            return null;
        }
        
        return find("policyId", policyId).firstResult();
    }
    
    /**
     * Setzt die EDC-Instanz für diese Policy.
     * 
     * @param instance Die EDC-Instanz, die dieser Policy zugeordnet werden soll
     */
    public void setEdcInstance(EdcInstance instance) {
        this.edcInstance = instance;
    }
    
    /**
     * Gibt die EDC-Instanz zurück, zu der diese Policy gehört.
     * 
     * @return Die EDC-Instanz dieser Policy
     */
    public EdcInstance getEdcInstance() {
        return this.edcInstance;
    }
    
    /**
     * Gibt die Policy-ID als String zurück.
     * 
     * @return Die Policy-ID
     */
    public String getPolicyId() {
        return this.policyId;
    }
    
    /**
     * Setzt die Policy-ID.
     * 
     * @param policyId Die zu setzende Policy-ID
     */
    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }
    
    /**
     * Gibt den Anzeigenamen der Policy zurück.
     * 
     * @return Der Anzeigename der Policy
     */
    public String getDisplayName() {
        return this.displayName;
    }
    
    /**
     * Setzt den Anzeigenamen der Policy.
     * 
     * @param displayName Der zu setzende Anzeigename
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Gibt die Policy-Definition als JSON-String zurück.
     * 
     * @return Die Policy-Definition als JSON-String
     */
    public String getPolicyJson() {
        return this.policyJson;
    }
    
    /**
     * Setzt die Policy-Definition als JSON-String.
     * 
     * @param policyJson Die zu setzende Policy-Definition als JSON-String
     */
    public void setPolicyJson(String policyJson) {
        this.policyJson = policyJson;
    }
}
