package de.unistuttgart.stayinsync.scriptengine.resultobject;

public class ValidationResult {

    private final boolean isValidScript;

    // TODO: Define a proper LoggerInfo Type
    private final Object loggerInfo;

    public ValidationResult(boolean isValidScript, Object loggerInfo) {
        this.isValidScript = isValidScript;
        this.loggerInfo = loggerInfo;
    }
    public boolean isValidScript() {
        return isValidScript;
    }
    public Object getLoggerInfo() {
        return loggerInfo;
    }
}
