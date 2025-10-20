package de.unistuttgart.stayinsync.core.configuration.edc.exception;

public class RequestBuildingException extends EdcException {

    Throwable throwable;

    public RequestBuildingException(String message, Throwable throwable) {
        super(message);
        this.throwable = throwable;
    }
}
