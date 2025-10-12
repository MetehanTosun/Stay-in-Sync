package de.unistuttgart.stayinsync.core.configuration.edc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

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
