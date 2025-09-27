package de.unistuttgart.stayinsync.pollingnode.exceptions.rabbitmqexceptions;


import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingNodeException;

public class ConsumerQueueUnbindingException extends PollingNodeException {

    public ConsumerQueueUnbindingException(String message, Throwable cause) {
        super(message, cause);
    }
}
