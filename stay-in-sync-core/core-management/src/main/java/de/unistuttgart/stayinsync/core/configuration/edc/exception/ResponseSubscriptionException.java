package de.unistuttgart.stayinsync.core.configuration.edc.exception;

public class ResponseSubscriptionException extends EdcException {

    Throwable throwable;

    public ResponseSubscriptionException(String message, Throwable throwable) {
        super(message);
        this.throwable = throwable;
    }
}
