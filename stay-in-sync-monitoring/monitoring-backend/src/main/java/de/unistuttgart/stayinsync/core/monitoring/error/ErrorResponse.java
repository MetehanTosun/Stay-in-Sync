package de.unistuttgart.stayinsync.core.monitoring.error;

import jakarta.ws.rs.core.Response;

import java.time.Instant;

public class ErrorResponse {
    public String error;
    public String message;
    public ErrorType type;
    public String timestamp;
    public String path;
    public Response.Status status;

    public ErrorResponse(String error, String message, ErrorType type, String path, Response.Status status) {
        this.error = error;
        this.message = message;
        this.type = type;
        this.path = path;
        this.status = status;
        this.timestamp = Instant.now().toString();
    }
}
