package de.unistuttgart.stayinsync.pollingnode.exceptions.execution;


import de.unistuttgart.stayinsync.core.pollingnode.exceptions.PollingNodeException;

public class InactivePollingJobCreationException extends PollingNodeException {
    public InactivePollingJobCreationException(String message) {
        super(message);
    }
}
