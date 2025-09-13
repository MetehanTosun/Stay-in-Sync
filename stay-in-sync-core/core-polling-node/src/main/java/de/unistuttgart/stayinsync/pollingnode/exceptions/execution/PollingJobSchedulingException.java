package de.unistuttgart.stayinsync.pollingnode.exceptions.execution;

import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingNodeException;

public class PollingJobSchedulingException extends PollingNodeException {
    public PollingJobSchedulingException(String message) {
        super(message);
    }

    public PollingJobSchedulingException(String message, Throwable cause){
        super(message, cause);
    }
}
