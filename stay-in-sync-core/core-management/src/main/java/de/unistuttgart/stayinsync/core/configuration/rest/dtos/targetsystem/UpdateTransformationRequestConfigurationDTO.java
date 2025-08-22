package de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * DTO to update the set of Target ARCs linked to a specific Transformation.
 */
public record UpdateTransformationRequestConfigurationDTO(
        @NotNull Set<Long> targetArcIds
) {
}
