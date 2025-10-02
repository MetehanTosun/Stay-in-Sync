package de.unistuttgart.stayinsync.core.configuration.rest.dtos.replay;

import java.util.Map;

public record ReplayExecuteResponseDTO(

        Object outputData,
        Map<String, Object> variables,
        String errorInfo) {
}
