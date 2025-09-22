package de.unistuttgart.stayinsync.core.transport.dto.targetsystems;

import java.util.List;

public record RequestConfigurationMessageDTO(
        String alias,
        String baseUrl,
        List<ActionMessageDTO> actions
) {
}
