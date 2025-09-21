package de.unistuttgart.stayinsync.pollingnode.exceptions.rabbitmqexceptions;

import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingNodeException;

public class ProducerPublishDataException extends PollingNodeException {
    public ProducerPublishDataException(String message) {
        super(message);
    }

    public ProducerPublishDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
