package de.unistuttgart.stayinsync.core.transport.dto;

import java.util.Map;

public record SyncDataMessageDTO(String arcAlias, Long requestConfigId, Map<String, Object> jsonData) {
}
