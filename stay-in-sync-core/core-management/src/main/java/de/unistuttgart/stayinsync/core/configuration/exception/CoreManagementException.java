package de.unistuttgart.stayinsync.core.configuration.exception;

/**
 * This class is supposed to be used for errors within the application business layer
 */
public class CoreManagementException extends Exception {

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
