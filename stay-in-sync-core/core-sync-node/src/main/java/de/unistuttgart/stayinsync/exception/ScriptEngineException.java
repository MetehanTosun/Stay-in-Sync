package de.unistuttgart.stayinsync.exception;

public class ScriptEngineException extends SyncNodeException {

    public enum ErrorType {
        SCRIPT_CACHING_ERROR,
        SCRIPT_PARSING_ERROR,
        CONTEXT_POOL_ERROR,
        SCRIPT_EXECUTION_ERROR,
        BINDING_ERROR,
        RESULT_EXTRACTION_ERROR,
        UNSUPPORTED_LANGUAGE_ERROR,
        CONFIGURATION_ERROR,
        SYNTAX_ERROR,
        RESOURCE_LIMIT_EXCEEDED,
    }

    private final ErrorType errorType;

    public ScriptEngineException(ErrorType errorType, String title, String message) {
        super(title, message);
        this.errorType = errorType;
    }

    public ScriptEngineException(ErrorType errorType, String title, String message, Throwable cause) {
        super(title, message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}