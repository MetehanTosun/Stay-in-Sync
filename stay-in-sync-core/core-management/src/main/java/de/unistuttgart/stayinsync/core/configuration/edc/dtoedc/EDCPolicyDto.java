package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Data Transfer Object (DTO) für EDC Policies.
 * Diese Klasse repräsentiert eine Policy im Eclipse Dataspace Connector (EDC) System.
 * Eine Policy definiert Zugriffsregeln und Bedingungen für Assets im EDC.
 * <p>
 * Das DTO wird für die Kommunikation zwischen Frontend, Backend und EDC verwendet
 * und beinhaltet alle relevanten Informationen einer Policy.
 */
public record EDCPolicyDto(
        /**
         * Die ID des DTOs (DB-ID). Wird in der JSON-Antwort mit ausgegeben,
         * damit das Frontend Updates/Löschungen adressieren kann.
         */
        Long id,
        
        /**
         * Die ID der EDC-Instanz, zu der diese Policy gehört.
         */
        Long edcId,
        
        /**
         * Die eindeutige ID der Policy im EDC-System.
         */
        @NotBlank
        String policyId,
        
        /**
         * Der Anzeigename der Policy für die Benutzeroberfläche.
         */
        String displayName,
        
        /**
         * Die eigentlichen Policy-Regeln und -Definitionen als Map.
         * Enthält die Berechtigungen, Verbote und Verpflichtungen gemäß ODRL-Standard.
         */
        @NotNull
        Map<String, Object> policy,
        
        /**
         * Rohe JSON-Repräsentation der Policy
         */
        String rawJson,
        
        /**
         * Der Kontext der Policy, wird als @context im JSON dargestellt.
         */
        @JsonProperty("@context")
        Map<String, String> context
) {
    /**
     * Default-Konstruktor.
     */
    public EDCPolicyDto {
        if (policy == null) {
            policy = new HashMap<>();
        }
        if (context == null) {
            context = new HashMap<>(Map.of("odrl", "http://www.w3.org/ns/odrl/2/"));
        }
    }
    
    /**
     * Factory-Methode für ein leeres EDCPolicyDto.
     * 
     * @return Ein leeres EDCPolicyDto mit Standardwerten
     */
    public static EDCPolicyDto createEmpty() {
        return new EDCPolicyDto(
                null,
                null,
                null,
                null,
                new HashMap<>(),
                null,
                new HashMap<>(Map.of("odrl", "http://www.w3.org/ns/odrl/2/"))
        );
    }
}
