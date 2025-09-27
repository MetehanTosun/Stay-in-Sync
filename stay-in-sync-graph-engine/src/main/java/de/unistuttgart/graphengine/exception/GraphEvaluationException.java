package de.unistuttgart.graphengine.exception;

/**
 * An exception thrown by the LogicGraphEvaluator or an Operation's execute() method
 * when a runtime error occurs during graph evaluation.
 * This is typically caused by invalid live data (e.g., wrong data types, missing values)
 * rather than a structural flaw in the graph itself.
 */
public class GraphEvaluationException extends LogicEngineException {

    /**
     * Defines the specific type of evaluation error that occurred.
     */
    public enum ErrorType {
        EXECUTION_FAILED,
        TYPE_MISMATCH,
        DATA_NOT_FOUND,
        INVALID_INPUT
    }

    private final ErrorType errorType;

    /**
     * Constructs a new GraphEvaluationException.
     *
     * @param errorType A specific error code for this exception.
     * @param title     A short, descriptive title for the error.
     * @param message   A human-readable message detailing the failure.
     * @param cause     The original exception that caused this failure, for debugging.
     */
    public GraphEvaluationException(ErrorType errorType, String title, String message, Throwable cause) {
        super(title, message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
