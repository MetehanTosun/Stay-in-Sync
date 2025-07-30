package de.unistuttgart.stayinsync.pollingnode.exceptions.execution;

public class UnsupportedRequestTypeException extends RuntimeException {
    public UnsupportedRequestTypeException(String message) {
        super(message);
    }
}
