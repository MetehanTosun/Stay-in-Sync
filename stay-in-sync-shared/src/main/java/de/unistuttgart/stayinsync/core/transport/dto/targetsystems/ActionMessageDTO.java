package de.unistuttgart.stayinsync.core.transport.dto.targetsystems;

import de.unistuttgart.stayinsync.core.transport.domain.TargetApiRequestConfigurationActionRole;

public record ActionMessageDTO(
        TargetApiRequestConfigurationActionRole actionRole,
        int executionOrder,
        String httpMethod,
        String path
) {
}
