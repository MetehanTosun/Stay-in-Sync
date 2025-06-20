package de.unistuttgart.stayinsync.core.configuration.exception;

/**
 * This class is supposed to be used for unrecoverable errors
 * within the application business layer
 */
public class CoreManagementException extends RuntimeException {

    private final String title;

    public CoreManagementException(String title, String message) {
        super(message);
        this.title = title;
    }

    public CoreManagementException(String title, String message, Throwable e) {
        super(message, e);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

}
