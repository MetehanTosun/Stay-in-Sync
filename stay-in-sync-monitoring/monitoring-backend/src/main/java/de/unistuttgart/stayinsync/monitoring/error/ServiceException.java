package de.unistuttgart.stayinsync.monitoring.error;

public class ServiceException extends RuntimeException {
    private final ErrorType errorType;

    public ServiceException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}