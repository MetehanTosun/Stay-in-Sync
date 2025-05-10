package de.unistuttgart.stayinsync.monitoring.error;

public class ErrorResponse {
    public String error;
    public String message;
    public ErrorType type;
    public String timestamp;

    public ErrorResponse(String error, String message, ErrorType type) {
        this.error = error;
        this.message = message;
        this.type = type;
        this.timestamp = java.time.Instant.now().toString();
    }
}