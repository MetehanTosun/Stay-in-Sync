package de.unistuttgart.stayinsync.core.transport.dto;

public record TransformationScriptDTO(
        Long id,
        String name,
        String hash,
        String javascriptCode,
        String generatedSdkCode,
        String generatedSdkHash
) {
}
