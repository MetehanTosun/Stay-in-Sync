package de.unistuttgart.stayinsync.scriptengine.message;

import java.util.ArrayList;
import java.util.List;

/**
 * We define a TransformationResult wrapper class in order to have standardized
 * outputData from a script execution
 * runtime. Additionally, there is loggerInformation to be used in Monitoring
 * and further steps after the evaluation
 * of the transformations.
 *
 * @author Maximilian Peresunchak
 * @since 1.0
 */
public class TransformationResult {
    private String jobId;
    private String scriptId;
    private boolean validExecution;
    private Object outputData;

    private final List<String> logMessages;
    private String errorInfo;

    /*
     * NEW: input JSON that the script received (sourceData) and the
     * transformationId for searching the snapshots later
     */
    private Object sourceData;
    private String transformationId;

    public TransformationResult(String jobId, String scriptId) {
        this.jobId = jobId;
        this.scriptId = scriptId;
        this.validExecution = false;
        this.logMessages = new ArrayList<>();
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    public boolean isValidExecution() {
        return validExecution;
    }

    public void setValidExecution(boolean validExecution) {
        this.validExecution = validExecution;
    }

    public Object getOutputData() {
        return outputData;
    }

    public void setOutputData(Object outputData) {
        this.outputData = outputData;
    }

    public List<String> getLogMessages() {
        return logMessages;
    }

    public void addLogMessage(String logMessage) {
        this.logMessages.add(logMessage);
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }

    public Object getSourceData() {
        return sourceData;
    }

    public void setSourceData(Object sourceData) {
        this.sourceData = sourceData;
    }

    public String getTransofrmationId() {
        return transformationId;
    }

    public void setTransformationId(String transformationId) {
        this.transformationId = transformationId;
    }

}
