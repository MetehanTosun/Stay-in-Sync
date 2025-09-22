package de.unistuttgart.stayinsync.core.monitoring.error;

public class ServiceException extends RuntimeException {
    private final ErrorType errorType;
    private final String path; // API endpoint

    public ServiceException(String message, ErrorType errorType) {
        this(message, errorType, null);
    }

    public ServiceException(String message, ErrorType errorType, String path) {
        super(message);
        this.errorType = errorType;
        this.path = path;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getPath() {
        return path;
    }
}
