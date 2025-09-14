package de.unistuttgart.stayinsync.core.configuration.rest.dtos.aas;

public record AasArcDTO(
        Long id,
        String alias,
        Long submodelId,
        String submodelIdShort,
        boolean active,
        int pollingIntervallTimeInMs,
        String arcType
) {
}
