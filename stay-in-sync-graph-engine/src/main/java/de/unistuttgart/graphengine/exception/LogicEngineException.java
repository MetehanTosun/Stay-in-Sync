package de.unistuttgart.graphengine.exception;

/**
 * The base exception for all errors originating from the logic engine.
 * <p>
 * This is an unchecked exception (extends RuntimeException) to provide maximum flexibility
 * in exception handling throughout the engine. While this makes all engine exceptions unchecked,
 * it allows for a consistent exception hierarchy where all engine-related errors can be caught
 * with a single catch block if desired.
 * <p>
 * Specific error scenarios are handled by specialized subclasses:
 * <ul>
 *   <li>{@link GraphConstructionException} - Graph construction and initialization errors</li>
 *   <li>{@link GraphSerializationException} - Serialization and hash computation errors</li>
 *   <li>{@link GraphEvaluationException} - Runtime evaluation errors</li>
 *   <li>{@link NodeConfigurationException} - Node configuration errors</li>
 * </ul>
 * <p>
 * All exceptions include a title field for structured error handling and reporting.
 */
public class LogicEngineException extends RuntimeException {

    private final String title;

    public LogicEngineException(String title, String message) {
        super(message);
        this.title = title;
    }

    public LogicEngineException(String title, String message, Throwable cause) {
        super(message, cause);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
