package de.unistuttgart.stayinsync.core.pollingnode.exceptions;

public class FaultySourceSystemApiRequestMessageDtoException extends Exception {
    public FaultySourceSystemApiRequestMessageDtoException(String message) {
        super(message);
    }

    public FaultySourceSystemApiRequestMessageDtoException(String message, Throwable cause) {
        super(message, cause);
    }
}
