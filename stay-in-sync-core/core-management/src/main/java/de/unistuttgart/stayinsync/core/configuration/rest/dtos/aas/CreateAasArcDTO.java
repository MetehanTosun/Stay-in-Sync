package de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateAasArcDTO(
        @NotNull Long sourceSystemId,
        @NotNull Long submodelId,
        @NotBlank String alias,
        boolean active,
        @Positive int pollingIntervallTimeInMs
) {
}
