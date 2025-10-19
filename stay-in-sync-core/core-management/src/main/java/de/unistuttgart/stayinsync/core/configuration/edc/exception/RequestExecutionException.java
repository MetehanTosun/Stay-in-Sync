package de.unistuttgart.stayinsync.core.configuration.edc.exception;

public class RequestExecutionException extends EdcException {

    Throwable throwable;

    public RequestExecutionException(String message, Throwable throwable) {
        super(message);
        this.throwable = throwable;
    }
}
