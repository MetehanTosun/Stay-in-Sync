package de.unistuttgart.stayinsync.core.configuration.edc.dtoedc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EDCAssetDto(
        UUID id,

        @NotBlank
        String assetId,

        @NotBlank
        String url,

        @NotBlank
        String type,

        @NotBlank
        String contentType,

        String description,

        @NotNull
        UUID targetEDCId,

        @NotNull
        EDCDataAddressDto dataAddress,

        EDCPropertyDto properties
) {}
