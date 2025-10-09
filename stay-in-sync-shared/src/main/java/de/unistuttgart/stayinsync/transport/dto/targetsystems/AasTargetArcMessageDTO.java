package de.unistuttgart.stayinsync.transport.dto.targetsystems;

public record AasTargetArcMessageDTO(
        String alias,
        String baseUrl,
        String submodelId
) {
}
