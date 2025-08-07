package de.unistuttgart.stayinsync.transport.dto.targetsystems;

import java.util.List;

public record RequestConfigurationMessageDTO(
        String alias,
        List<ActionMessageDTO> actions
) {
}
