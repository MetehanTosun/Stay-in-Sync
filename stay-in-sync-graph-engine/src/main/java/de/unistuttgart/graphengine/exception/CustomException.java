package de.unistuttgart.stayinsync.transport.exception;

/**
 * Custom exception class for handling application-specific errors.
 */
public class CustomException extends Exception {

    public CustomException(String message) {
        super(message);
    }

    public CustomException(String message, Throwable cause) {
        super(message, cause);
    }
}
