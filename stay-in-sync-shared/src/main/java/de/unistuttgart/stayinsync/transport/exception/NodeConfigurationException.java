package de.unistuttgart.stayinsync.transport.exception;

/**
 * An exception thrown when a Node is constructed with invalid or incomplete configuration.
 * This indicates a flaw in the graph's definition itself.
 */
public class NodeConfigurationException extends IllegalArgumentException {

    public NodeConfigurationException(String message) {
        super(message);
    }
}
