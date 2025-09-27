package de.unistuttgart.graphengine.exception;

/**
 * An exception thrown when a Node is constructed with invalid or incomplete configuration.
 * This indicates a flaw in the graph's definition itself.
 */
public class NodeConfigurationException extends LogicEngineException {

    /**
     * Constructs a new NodeConfigurationException.
     *
     * @param message A detailed message explaining the configuration failure.
     */
    public NodeConfigurationException(String message) {
        super("Invalid Node Configuration", message);
    }
}
