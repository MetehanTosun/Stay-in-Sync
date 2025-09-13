package de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob;


import de.unistuttgart.stayinsync.core.pollingnode.exceptions.PollingNodeException;

public class PollingJobException extends PollingNodeException {

    public PollingJobException() {
        super();
    }

    public PollingJobException(String message) {
        super(message);
    }

    public PollingJobException(Throwable cause) {
        super(cause);
    }

    public PollingJobException(String message, Throwable cause) {
        super(message, cause);
    }
}
