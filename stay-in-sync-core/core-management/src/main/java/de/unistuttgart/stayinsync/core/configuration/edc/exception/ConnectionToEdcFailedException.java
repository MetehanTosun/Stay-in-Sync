package de.unistuttgart.stayinsync.core.configuration.edc.exception;

public class ConnectionToEdcFailedException extends EdcException{

    Throwable throwable;

    public ConnectionToEdcFailedException(String message, Throwable throwable){
        super(message);
        this.throwable = throwable;
    }

    public ConnectionToEdcFailedException(String message){
        super(message);
    }
}
