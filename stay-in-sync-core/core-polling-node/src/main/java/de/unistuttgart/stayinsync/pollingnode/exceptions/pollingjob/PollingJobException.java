package de.unistuttgart.stayinsync.pollingnode.exceptions.pollingjob;

import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingNodeException;

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
