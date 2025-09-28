package de.unistuttgart.stayinsync.core.configuration.rest.dtos.replay;

import com.fasterxml.jackson.databind.JsonNode;

public record ReplayExecuteRequestDTO(
        String scriptName,
        String javascriptCode,
        JsonNode sourceData) {
}