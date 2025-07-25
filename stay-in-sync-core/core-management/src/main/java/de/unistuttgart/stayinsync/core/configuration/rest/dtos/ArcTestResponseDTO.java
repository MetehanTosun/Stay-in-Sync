package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

public record ArcTestResponseDTO(
        boolean isSuccess,
        int httpStatusCode,
        Object responsePayload,
        String generatedDts,
        String errorMessage
) {
}
