package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

public record TransformationScriptDTO(
        Long id,
        String name,
        String hash,
        String typescriptCode,
        String javascriptCode
) {
}
