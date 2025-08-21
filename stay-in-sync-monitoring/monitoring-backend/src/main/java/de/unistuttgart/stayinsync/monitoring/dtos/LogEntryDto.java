package de.unistuttgart.stayinsync.monitoring.dtos;


public record LogEntryDto(String service, String level, String message, String timestamp) {
}

