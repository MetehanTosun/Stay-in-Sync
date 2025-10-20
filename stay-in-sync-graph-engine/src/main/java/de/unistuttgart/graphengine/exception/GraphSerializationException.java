package de.unistuttgart.graphengine.exception;

/**
 * An exception thrown when graph serialization, deserialization, or hash computation fails.
 * <p>
 * This exception extends {@link LogicEngineException}, making it an unchecked exception.
 * This is particularly useful for operations that occur during initialization (e.g., in ThreadLocal)
 * where checked exceptions cannot be thrown.
 * <p>
 * Common scenarios:
 * <ul>
 *   <li>Hash computation during cache initialization</li>
 *   <li>JSON serialization/deserialization failures</li>
 *   <li>Cryptographic algorithm unavailability (SHA-256)</li>
 * </ul>
 */
public class GraphSerializationException extends LogicEngineException {

    /**
     * Defines the specific type of serialization error that occurred.
     */
    public enum ErrorType {
        SERIALIZATION_FAILED,
        DESERIALIZATION_FAILED,
        HASH_COMPUTATION_FAILED,
        INVALID_FORMAT
    }

    private final ErrorType errorType;

    /**
     * Constructs a new GraphSerializationException.
     *
     * @param errorType A specific error code for this exception.
     * @param message   A human-readable message detailing the failure.
     * @param cause     The original exception that caused this failure.
     */
    public GraphSerializationException(ErrorType errorType, String message, Throwable cause) {
        super("Graph Serialization Error", message, cause);
        this.errorType = errorType;
    }

    /**
     * Constructs a new GraphSerializationException without a cause.
     *
     * @param errorType A specific error code for this exception.
     * @param message   A human-readable message detailing the failure.
     */
    public GraphSerializationException(ErrorType errorType, String message) {
        super("Graph Serialization Error", message);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
