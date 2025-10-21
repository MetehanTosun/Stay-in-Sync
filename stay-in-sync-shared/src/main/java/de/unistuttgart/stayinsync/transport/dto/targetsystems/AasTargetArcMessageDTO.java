package de.unistuttgart.stayinsync.transport.dto.targetsystems;

import de.unistuttgart.stayinsync.transport.dto.ApiRequestHeaderMessageDTO;

import java.util.List;

public record AasTargetArcMessageDTO(
        String alias,
        String baseUrl,
        String submodelId,
        List<ApiRequestHeaderMessageDTO> headers
) {
}
