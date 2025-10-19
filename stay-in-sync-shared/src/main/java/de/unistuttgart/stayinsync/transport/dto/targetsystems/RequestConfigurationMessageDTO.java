package de.unistuttgart.stayinsync.transport.dto.targetsystems;

import de.unistuttgart.stayinsync.transport.dto.ApiRequestHeaderMessageDTO;

import java.util.List;

public record RequestConfigurationMessageDTO(
        Long id,
        String alias,
        String baseUrl,
        List<ActionMessageDTO> actions,
        List<ApiRequestHeaderMessageDTO> headers
) {
}
