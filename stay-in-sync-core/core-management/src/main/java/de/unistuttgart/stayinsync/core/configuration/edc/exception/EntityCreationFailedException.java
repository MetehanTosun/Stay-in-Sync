package de.unistuttgart.stayinsync.core.configuration.edc.exception;


public class EntityCreationFailedException extends EdcException{

    Throwable throwable;

    public EntityCreationFailedException(String message, Throwable throwable) {
        super(message);
        this.throwable = throwable;
    }
}
