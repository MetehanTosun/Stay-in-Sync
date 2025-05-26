package de.unistuttgart.stayinsync.exception;

public class SyncNodeException extends RuntimeException {
    private final String title;

    public SyncNodeException(String title, String message) {
        super(message);
        this.title = title;
    }

    public SyncNodeException(String title, String message, Throwable cause) {
        super(message, cause);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
