package de.unistuttgart.stayinsync.transport.transformation_rule_shared.validation_error;

/**
 * An interface representing a single validation failure in a logic graph.
 * It provides a user-friendly message and an error code for the frontend.
 */
public interface ValidationError {

    /**
     * @return A machine-readable error code (e.g., "CYCLE_DETECTED").
     */
    String getErrorCode();

    /**
     * @return A human-readable message describing the error.
     */
    String getMessage();
}
