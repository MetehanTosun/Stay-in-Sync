package de.unistuttgart.stayinsync.core.configuration.edc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
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
public record PolicyDefinitionDto (
        @JsonView(VisibilitySidesForDto.Ui.class)
        String displayName,
        @JsonProperty("@context")
        ContextDto context,
        //TODO Im Mapper auf PolicyDefinition festsetzen
        @JsonProperty("@type")
        String type,
        @JsonProperty("@id")
        String policyDefinitionId,
        PolicyDto policy
) {
    



}
