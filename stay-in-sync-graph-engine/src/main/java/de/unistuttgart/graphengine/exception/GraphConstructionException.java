package de.unistuttgart.graphengine.exception;

/**
 * A runtime exception thrown when a graph cannot be constructed or initialized
 * due to structural or configuration problems.
 * <p>
 * This extends LogicEngineException, making it part of the unified exception
 * hierarchy for the graph engine.
 * <p>
 * Common scenarios include:
 * <ul>
 *   <li>Missing required nodes (e.g., ConfigNode, FinalNode)</li>
 *   <li>Multiple instances of nodes that should be unique</li>
 *   <li>Invalid graph structure (e.g., disconnected nodes)</li>
 *   <li>Null or empty graph definitions</li>
 * </ul>
 *
 * @see NodeConfigurationException for checked exceptions during node creation
 */
public class GraphConstructionException extends LogicEngineException {

    /**
     * Defines the specific type of construction error that occurred.
     */
    public enum ErrorType {
        MISSING_REQUIRED_NODE,
        DUPLICATE_NODE,
        INVALID_STRUCTURE,
        EMPTY_GRAPH,
        NULL_INPUT
    }

    private final ErrorType errorType;

    /**
     * Constructs a new GraphConstructionException.
     *
     * @param errorType A specific error code for this exception.
     * @param message   A human-readable message detailing the failure.
     */
    public GraphConstructionException(ErrorType errorType, String message) {
        super("Graph Construction Error", message);
        this.errorType = errorType;
    }

    /**
     * Constructs a new GraphConstructionException with a cause.
     *
     * @param errorType A specific error code for this exception.
     * @param message   A human-readable message detailing the failure.
     * @param cause     The original exception that caused this failure.
     */
    public GraphConstructionException(ErrorType errorType, String message, Throwable cause) {
        super("Graph Construction Error", message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
