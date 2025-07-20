package de.unistuttgart.stayinsync.pollingnode.exceptions;

public class FaultySourceSystemApiRequestMessageDtoException extends Exception {
    public FaultySourceSystemApiRequestMessageDtoException(String message) {
        super(message);
    }

    public FaultySourceSystemApiRequestMessageDtoException(String message, Throwable cause) {
        super(message, cause);
    }
}
