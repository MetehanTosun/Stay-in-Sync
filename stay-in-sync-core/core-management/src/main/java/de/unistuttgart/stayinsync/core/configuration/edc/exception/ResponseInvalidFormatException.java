package de.unistuttgart.stayinsync.core.configuration.edc.exception;

public class ResponseInvalidFormatException extends EdcException {

    Throwable throwable;

    public ResponseInvalidFormatException(String message, Throwable throwable) {
        super(message);
        this.throwable = throwable;
    }
}
