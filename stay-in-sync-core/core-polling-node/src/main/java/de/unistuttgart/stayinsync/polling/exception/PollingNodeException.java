package de.unistuttgart.stayinsync.polling.exception;

public class PollingNodeException extends Exception {

    private final String title;

    public PollingNodeException(String title, String message) {
        super(message);
        this.title = title;
    }

    public PollingNodeException(String title, String message, Throwable e) {
        super(message, e);
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

}
