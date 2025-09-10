package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Datenübertragungsobjekt (DTO) für EDC Policies.
 * Diese Klasse repräsentiert eine Policy im Eclipse Dataspace Connector (EDC) System.
 * Eine Policy definiert Zugriffsregeln und Bedingungen für Assets im EDC.

 * Das DTO wird für die Kommunikation zwischen Frontend, Backend und EDC verwendet
 * und beinhaltet alle relevanten Informationen einer Policy.
 */
public record EDCPolicyDto (UUID edcId, String policyId, String displayName,
                            Map<String,Object> policy)
{
    }
