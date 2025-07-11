package de.unistuttgart.stayinsync.transport.dto;

import java.util.Set;

public record SyncJobMessageDTO(Long id, String name, boolean deployed,
                                Set<TransformationMessageDTO> transformations) {
}
