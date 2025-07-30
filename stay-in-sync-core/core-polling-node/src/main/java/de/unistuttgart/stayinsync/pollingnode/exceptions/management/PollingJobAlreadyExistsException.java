package de.unistuttgart.stayinsync.pollingnode.exceptions.management;

import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingNodeException;

public class PollingJobAlreadyExistsException extends PollingNodeException {

    Long id;

    public PollingJobAlreadyExistsException(String message) {
        super(message);
    }

    public PollingJobAlreadyExistsException(String message, Long id){
        super(message);
        this.id = id;
    }
}
