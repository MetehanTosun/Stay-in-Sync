package de.unistuttgart.stayinsync.transport.exception;

/**
 * A specific runtime exception thrown by an Operation's validateNode() method
 * when the node's configuration is invalid for that specific operator
 * (e.g., wrong number of inputs).
 */
public class OperatorValidationException extends IllegalArgumentException {

    public OperatorValidationException(String message) {
        super(message);
    }
}
