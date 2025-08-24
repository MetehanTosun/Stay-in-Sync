package de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.restclientexceptions;

import de.unistuttgart.stayinsync.pollingnode.exceptions.execution.pollingjob.PollingJobException;

public class ResponseInvalidFormatException extends PollingJobException {
    public ResponseInvalidFormatException(String message, Throwable cause){
        super(message,cause);
    }
}
