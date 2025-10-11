package de.unistuttgart.graphengine.exception;

/**
 * An exception thrown when graph serialization, deserialization, or hash computation fails.
 * This typically occurs when a graph cannot be converted to/from JSON or when
 * cryptographic operations fail during hash generation.
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
