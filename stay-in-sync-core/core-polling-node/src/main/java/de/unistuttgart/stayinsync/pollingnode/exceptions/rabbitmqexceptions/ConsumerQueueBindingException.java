package de.unistuttgart.stayinsync.pollingnode.exceptions.rabbitmqexceptions;


import de.unistuttgart.stayinsync.pollingnode.exceptions.PollingNodeException;

public class ConsumerQueueBindingException extends PollingNodeException {
    public ConsumerQueueBindingException(String message, Throwable cause) {
        super(message, cause);
    }
}
