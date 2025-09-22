package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import de.unistuttgart.stayinsync.core.transport.ScriptStatus;

import java.util.Set;

public record TransformationScriptDTO(
        Long id,
        String name,
        String typescriptCode,
        String javascriptCode,
        Set<String> requiredArcAliases,
        ScriptStatus status,
        Set<Long> targetArcIds
) {
}
