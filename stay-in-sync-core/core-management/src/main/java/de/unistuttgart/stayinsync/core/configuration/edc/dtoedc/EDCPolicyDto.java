package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Datenübertragungsobjekt (DTO) für EDC Policies.
 * 
 * Diese Klasse repräsentiert eine Policy im Eclipse Dataspace Connector (EDC) System.
 * Eine Policy definiert Zugriffsregeln und Bedingungen für Assets im EDC.
 * 
 * Das DTO wird für die Kommunikation zwischen Frontend, Backend und EDC verwendet
 * und beinhaltet alle relevanten Informationen einer Policy.
 */
public class EDCPolicyDto {
    /**
     * Die eindeutige UUID dieser Policy in der Datenbank.
     * Wird automatisch generiert, wenn nicht gesetzt.
     */
    private UUID id;

    /**
     * Die Policy-ID als String, wie sie im EDC verwendet wird.
     * Beispiel: "my-policy-id" oder "policy-for-asset-123"
     * Muss eindeutig sein und darf nicht leer sein.
     */
    @NotBlank
    private String policyId;

    /**
     * Ein optionaler Anzeigename für die Policy.
     * Dieser wird im Frontend zur übersichtlicheren Darstellung verwendet.
     */
    private String displayName;

    /**
     * Die vollständige Policy-Definition als Map-Struktur.
     * Enthält die komplette JSON-Struktur der Policy nach EDC-Standard,
     * inklusive @context, permission, prohibition, obligation, etc.
     */
    @NotNull
    private Map<String, Object> policy;

    // --- Getter und Setter ---
    
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }
    
    public String getDisplayName() {
        return displayName != null ? displayName : policyId;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Map<String, Object> getPolicy() {
        return policy;
    }

    public void setPolicy(Map<String, Object> policy) {
        this.policy = policy;
    }
}
