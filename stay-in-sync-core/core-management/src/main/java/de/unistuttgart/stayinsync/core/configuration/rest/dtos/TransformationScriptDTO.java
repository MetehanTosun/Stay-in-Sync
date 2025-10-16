package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

import java.util.Set;

import de.unistuttgart.stayinsync.transport.ScriptStatus;

public record TransformationScriptDTO(
                Long id,
                String name,
                String typescriptCode,
                String javascriptCode,
                Set<String> requiredArcAliases,
                ScriptStatus status,
                Set<Long> restTargetArcIds,
                Set<Long> aasTargetArcIds,
                String generatedSdkCode) {
}
