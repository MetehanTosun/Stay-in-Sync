package de.unistuttgart.stayinsync.scriptengine.message;


/**
 * This class wraps an evaluation result from an executed synchronisation logic script and offers additional
 * information in form of loggerInfo.
 *
 * @since 1.0
 * @author Maximilian Peresunchak
 */
public class ConditionResult {

    private final boolean isValidEvaluation;

    // TODO: Define a proper LoggerInfo Type
    private Object loggerInfo;

    public ConditionResult(boolean isValidEvaluation, Object loggerInfo) {
        this.isValidEvaluation = isValidEvaluation;
        this.loggerInfo = loggerInfo;
    }

    public boolean isValidEvaluation() {
        return isValidEvaluation;
    }

    public Object getLoggerInfo() {
        return loggerInfo;
    }

    public void setLoggerInfo(Object loggerInfo) {
        this.loggerInfo = loggerInfo;
    }
}
