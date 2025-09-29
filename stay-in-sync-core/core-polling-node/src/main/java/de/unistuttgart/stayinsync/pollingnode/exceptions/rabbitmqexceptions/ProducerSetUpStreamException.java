package de.unistuttgart.stayinsync.pollingnode.exceptions.rabbitmqexceptions;


import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingNodeException;

public class ProducerSetUpStreamException extends PollingNodeException {
    public ProducerSetUpStreamException(String message) {
        super(message);
    }

    public ProducerSetUpStreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
