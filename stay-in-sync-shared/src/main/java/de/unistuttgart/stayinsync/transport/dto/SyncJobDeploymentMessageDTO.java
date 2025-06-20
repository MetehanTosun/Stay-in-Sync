package de.unistuttgart.stayinsync.transport.dto;

import java.util.Set;

public record SyncJobDeploymentMessageDTO(
        String name,
        Set<TransformationMessageDTO> transformations
) {


}
