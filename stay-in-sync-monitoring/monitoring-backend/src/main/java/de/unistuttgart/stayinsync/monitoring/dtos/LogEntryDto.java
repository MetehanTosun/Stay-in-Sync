package de.unistuttgart.stayinsync.monitoring.dtos;

/**
 * Data Transfer Object representing a single log entry.
 *
 * This record is immutable and provides a simple way to transfer log data
 * between different layers of the application or across services.
 *
 * Fields:
 * @param service          The name of the service that created the log entry.
 * @param level            The log level
 * @param message          The actual log message text.
 * @param timestamp        The timestamp when the log entry was created.
 * @param transformationId An optional identifier to group logs belonging
 *                         to the same transformation or process.
 */
public record LogEntryDto(
        String service,
        String level,
        String message,
        String timestamp,
        String transformationId
) {
}
