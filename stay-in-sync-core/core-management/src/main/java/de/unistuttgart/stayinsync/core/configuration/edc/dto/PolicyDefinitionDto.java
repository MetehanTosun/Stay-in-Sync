package de.unistuttgart.stayinsync.core.configuration.edc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Data Transfer Object (DTO) für EDC Policies.
 * Diese Klasse repräsentiert eine Policy im Eclipse Dataspace Connector (EDC) System.
 * Eine Policy definiert Zugriffsregeln und Bedingungen für Assets im EDC.
 * 
 * Das DTO wird für die Kommunikation zwischen Frontend, Backend und EDC verwendet
 * und beinhaltet alle relevanten Informationen einer Policy.
 */
public class PolicyDefinitionDto {
    
    /**
     * Die ID des DTOs (DB-UUID). Wird in der JSON-Antwort mit ausgegeben,
     * damit das Frontend Updates/Löschungen adressieren kann.
     */
    private UUID id;

    /**
     * Die ID der EDC-Instanz, zu der diese Policy gehört.
     */
    private UUID edcId;

    /**
     * Die eindeutige ID der Policy im EDC-System.
     */
    @NotBlank
    @JsonProperty("@id")
    private String policyId;

    /**
     * Der Anzeigename der Policy für die Benutzeroberfläche.
     */
    private String displayName;

    /**
     * Die eigentlichen Policy-Regeln und -Definitionen als Map.
     * Enthält die Berechtigungen, Verbote und Verpflichtungen gemäß ODRL-Standard.
     */
    @NotNull
    private Map<String, Object> policy;

    /**
     * Der Kontext der Policy, wird als @context im JSON dargestellt.
     */
    @JsonProperty("@context")
    private Map<String, String> context;

    /**
     * Default-Konstruktor.
     */
    public PolicyDefinitionDto() {
        this.policy = new HashMap<>();
        this.context = new HashMap<>(Map.of("odrl", "http://www.w3.org/ns/odrl/2/"));
    }

    /**
     * Vollständiger Konstruktor.
     * 
     * @param id Die ID des DTOs
     * @param edcId Die ID der EDC-Instanz
     * @param policyId Die ID der Policy
     * @param displayName Der Anzeigename der Policy
     * @param policy Die Policy-Regeln als Map
     * @param context Der Kontext der Policy
     */
    public PolicyDefinitionDto(UUID id, UUID edcId, String policyId, String displayName,
                               Map<String, Object> policy, Map<String, String> context) {
        this.id = id;
        this.edcId = edcId;
        this.policyId = policyId;
        this.displayName = displayName;
        this.policy = policy != null ? policy : new HashMap<>();
        this.context = context != null ? context : new HashMap<>(Map.of("odrl", "http://www.w3.org/ns/odrl/2/"));
    }

    /**
     * Konstruktor mit minimalen erforderlichen Parametern.
     * 
     * @param edcId Die ID der EDC-Instanz
     * @param policyId Die ID der Policy
     * @param displayName Der Anzeigename der Policy
     * @param policy Die Policy-Regeln als Map
     */
    public PolicyDefinitionDto(UUID edcId, String policyId, String displayName, Map<String, Object> policy) {
        this(null, edcId, policyId, displayName, policy, new HashMap<>(Map.of("odrl", "http://www.w3.org/ns/odrl/2/")));
    }

    // Getter und Setter

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEdcId() {
        return edcId;
    }

    public void setEdcId(UUID edcId) {
        this.edcId = edcId;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Map<String, Object> getPolicy() {
        return policy;
    }

    public void setPolicy(Map<String, Object> policy) {
        this.policy = policy != null ? policy : new HashMap<>();
    }

    public Map<String, String> getContext() {
        return context;
    }

    public void setContext(Map<String, String> context) {
        this.context = context != null ? context : new HashMap<>(Map.of("odrl", "http://www.w3.org/ns/odrl/2/"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolicyDefinitionDto that = (PolicyDefinitionDto) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(edcId, that.edcId) &&
               Objects.equals(policyId, that.policyId) &&
               Objects.equals(displayName, that.displayName) &&
               Objects.equals(policy, that.policy) &&
               Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, edcId, policyId, displayName, policy, context);
    }

    @Override
    public String toString() {
        return "EDCPolicyDto{" +
                "id=" + id +
                ", edcId=" + edcId +
                ", policyId='" + policyId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", policy=" + policy +
                ", context=" + context +
                '}';
    }
}
