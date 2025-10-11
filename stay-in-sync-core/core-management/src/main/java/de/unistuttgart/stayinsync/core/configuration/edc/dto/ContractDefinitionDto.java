package de.unistuttgart.stayinsync.core.configuration.edc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;
import java.util.UUID;

public record ContractDefinitionDto(
        @JsonProperty("@context")
        ContextDto context,
        @JsonProperty("@type")
        String type,
        @JsonProperty("@id")
        String contractDefinitionId,
        String accessPolicyId,
        String contractPolicyId,
        Map<String,Object> assetsSelector

) implements EdcEntityDto {

}
