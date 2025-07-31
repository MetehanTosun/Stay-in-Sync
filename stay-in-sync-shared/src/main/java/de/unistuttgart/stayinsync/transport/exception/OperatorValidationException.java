package de.unistuttgart.stayinsync.transport.exception;

/**
 * A specific runtime exception thrown by an Operation's validateNode() method
 * when the node's configuration is invalid for that specific operator
 * (e.g., wrong number of inputs).
 */
public class OperatorValidationException extends LogicEngineException {

    /**
     * Constructs a new OperatorValidationException with a default title.
     * This constructor is designed to be called with a single, fully-formed error message
     * from within an operator's validation logic.
     *
     * @param message A detailed message explaining the specific validation failure.
     */
    public OperatorValidationException(String message) {
        super("Invalid Operator Configuration", message);
    }
}
