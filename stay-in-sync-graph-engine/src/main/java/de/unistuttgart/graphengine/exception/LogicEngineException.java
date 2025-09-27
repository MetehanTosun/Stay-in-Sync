package de.unistuttgart.graphengine.exception;

/**
 * The base exception for all errors originating from the logic engine.
 * It follows the custom exception pattern by including a title for structured error handling.
 */
public class LogicEngineException extends Exception {

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
