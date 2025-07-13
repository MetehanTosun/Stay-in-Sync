package de.unistuttgart.stayinsync.transport.dto;

import java.util.Map;

public record SyncDataMessageDTO(Long requestConfigId, Map<String, Object> jsonData) {
}
