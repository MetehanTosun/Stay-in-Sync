package de.unistuttgart.stayinsync.scriptengine.resultobject;

public class IntegrityResult {

    public final boolean isValidData;

    // TODO: Define a proper LoggerInfo Type
    public final Object loggerInfo;

    public IntegrityResult(boolean isValidData, Object loggerInfo) {
        this.isValidData = isValidData;
        this.loggerInfo = loggerInfo;
    }

    public boolean isValidData() {
        return isValidData;
    }

    public Object getLoggerInfo() {
        return loggerInfo;
    }
}
