package de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAasTargetArcDTO(
        @NotNull Long targetSystemId,
        @NotNull Long submodelId,
        @NotBlank String alias
) {
}
