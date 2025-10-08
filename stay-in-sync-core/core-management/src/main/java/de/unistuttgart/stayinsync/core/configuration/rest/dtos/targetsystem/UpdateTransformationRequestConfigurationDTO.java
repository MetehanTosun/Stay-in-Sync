package de.unistuttgart.stayinsync.core.configuration.rest.dtos.targetsystem;

import java.util.Set;

/**
 * DTO to update the set of Target ARCs linked to a specific Transformation.
 */
public record UpdateTransformationRequestConfigurationDTO(
        Set<Long> restTargetArcIds,
        Set<Long> aasTargetArcIds
) {
}
