package de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas;

public record AasTargetArcDTO(
        Long id,
        String alias,
        Long targetSystemId,
        String targetSystemName,
        Long submodelId,
        String submodelIdShort,
        String arcType
) {
}
