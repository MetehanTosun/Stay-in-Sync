package de.unistuttgart.stayinsync.pollingnode;

import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingNodeException;

public class HttpRequestExecutionException extends PollingNodeException {
    public HttpRequestExecutionException(String message) {
        super(message);
    }
}
