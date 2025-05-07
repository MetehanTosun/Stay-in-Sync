package de.unistuttgart.stayinsync.scriptengine.resultobject;

/**
 * We define a TransformationResult wrapper class in order to have standardized outputData from a script execution
 * runtime. Additionally, there is loggerInformation to be used in Monitoring and further steps after the evaluation
 * of the transformations.
 *
 * @author Maximilian Peresunchak
 * @since 1.0
 */
public class TransformationResult implements ResultEvaluation{

    private boolean isValidExecution;

    // TODO: Define proper OutputData Type
    private Object outputData;

    // TODO: Define a proper LoggerInfo Type
    private Object loggerInfo;

    public TransformationResult() {

    }

    public boolean isValidExecution() {
        return isValidExecution;
    }

    public void setValidExecution(boolean validExecution) {
        isValidExecution = validExecution;
    }

    public Object getOutputData() {
        return outputData;
    }
    public void setOutputData(Object outputData) {
        this.outputData = outputData;
    }

    public Object getLoggerInfo() {
        return loggerInfo;
    }

    public void setLoggerInfo(Object loggerInfo) {
        this.loggerInfo = loggerInfo;
    }
}
