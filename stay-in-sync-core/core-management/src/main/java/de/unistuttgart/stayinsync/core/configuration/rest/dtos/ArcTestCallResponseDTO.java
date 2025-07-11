package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

public record ArcTestCallResponseDTO(
        boolean isSuccess,
        int httpStatusCode,
        Object responsePayload, // TODO: Decide on Jackson's `ObjectNode` or `Map<String, Object>` for the raw JSON
        String generatedDts,
        String errorMessage
) {
}
