package de.unistuttgart.stayinsync.core.pollingnode;

import de.unistuttgart.stayinsync.core.pollingnode.exceptions.PollingNodeException;

public class HttpRequestExecutionException extends PollingNodeException {
    public HttpRequestExecutionException(String message) {
        super(message);
    }
}
