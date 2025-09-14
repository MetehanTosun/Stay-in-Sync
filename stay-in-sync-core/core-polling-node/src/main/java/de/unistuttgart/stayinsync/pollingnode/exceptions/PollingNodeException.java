package de.unistuttgart.stayinsync.pollingnode.exceptions;

public class PollingNodeException extends Exception {

    public PollingNodeException() {
        super();
    }

    public PollingNodeException(String message) {
        super(message);
    }

    public PollingNodeException(Throwable cause) {
        super(cause);
    }

    public PollingNodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
