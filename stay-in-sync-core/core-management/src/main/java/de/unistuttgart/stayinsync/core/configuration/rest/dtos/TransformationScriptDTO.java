package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import java.util.Set;

public record TransformationScriptDTO(
        Long id,
        String name,
        String hash,
        String typescriptCode,
        String javascriptCode,
        Set<String> requiredArcAliases
) {
}
