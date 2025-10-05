package de.unistuttgart.stayinsync.transport.dto.targetsystems;

import java.util.List;

public record RequestConfigurationMessageDTO(
        Long id,
        String alias,
        String baseUrl,
        List<ActionMessageDTO> actions
) {
}
