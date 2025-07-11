package de.unistuttgart.stayinsync.core.configuration.rest.dtos;

public record ArcTestResponseDTO(
        boolean isSuccess,
        int httpStatusCode,
        io.vertx.core.json.JsonObject responsePayload,
        String generatedDts,
        String errorMessage
) {
}
