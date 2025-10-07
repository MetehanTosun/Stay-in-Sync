package de.unistuttgart.stayinsync.core.configuration.edc.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record EDCInstanceDto(
        UUID id,
        @NotBlank String name,
        @NotBlank String controlPlaneManagementUrl,
        String protocolVersion,
        String description,
        String bpn,
        String apiKey,
        String edcAssetEndpoint,
        String edcPolicyEndpoint,
        String edcContractDefinitionEndpoint
        ) {

}
