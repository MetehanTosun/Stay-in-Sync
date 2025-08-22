package de.unistuttgart.stayinsync.transport.dto.targetsystems;

import de.unistuttgart.stayinsync.transport.domain.TargetApiRequestConfigurationActionRole;

public record ActionMessageDTO(
        TargetApiRequestConfigurationActionRole actionRole,
        int executionOrder,
        String httpMethod,
        String path
) {
}
