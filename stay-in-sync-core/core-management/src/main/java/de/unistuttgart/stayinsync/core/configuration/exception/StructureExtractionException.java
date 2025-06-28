package de.unistuttgart.stayinsync.core.configuration.exception;

/**
 * Exception thrown when JSON schema extraction fails for a SourceSystemEndpoint.
 */
public class StructureExtractionException extends RuntimeException {

    /**
     * Constructs a new StructureExtractionException with the specified detail message.
     *
     * @param message the detail message
     */
    public StructureExtractionException(String message) {
        super(message);
    }

    /**
     * Constructs a new StructureExtractionException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public StructureExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
