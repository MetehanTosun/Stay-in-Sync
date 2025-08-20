package de.unistuttgart.stayinsync.monitoring.dtos;


public class LogEntryDto {
    private final long timestamp;
    private final String message;
    private final String rawMessage;
    private final String level;
    private final String caller;

    public LogEntryDto(String timestamp, String message, String rawMessage, String level, String caller) {
        this.timestamp = Long.parseLong(timestamp);
        this.message = message;
        this.rawMessage = rawMessage;
        this.level = level;
        this.caller = caller;
    }

    // Getter & Setter
    public long getTimestamp() { return timestamp; }
    public String getMessage() { return message; }
    public String getRawMessage() { return rawMessage; }
    public String getLevel() { return level; }
    public String getCaller() { return caller; }
}
