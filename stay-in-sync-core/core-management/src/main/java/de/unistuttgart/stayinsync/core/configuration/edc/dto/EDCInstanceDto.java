package de.unistuttgart.stayinsync.core.configuration.edc.dto;

import jakarta.validation.constraints.NotBlank;



public record EDCInstanceDto(
        Long id,
        @NotBlank String name,
        String controlPlaneManagementUrl,
        String protocolVersion,
        String description,
        String bpn,
        String apiKey
) {

}
