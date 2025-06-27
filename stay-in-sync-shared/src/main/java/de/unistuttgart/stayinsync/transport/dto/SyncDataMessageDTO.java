package de.unistuttgart.stayinsync.transport.dto;

import java.util.Map;

public record SyncDataMessageDTO(Long endpointId, Map<String, Object> jsonData) {
}
