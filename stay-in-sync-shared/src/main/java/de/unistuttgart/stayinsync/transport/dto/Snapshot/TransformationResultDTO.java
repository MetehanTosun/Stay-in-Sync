package de.unistuttgart.stayinsync.transport.dto.Snapshot;

import com.fasterxml.jackson.databind.JsonNode;

/** Pure-DTO for API and persistence. */
public class TransformationResultDTO {
    private String transformationId;

    private String jobId;
    private String scriptId;
    private Boolean validExecution;

    private JsonNode sourceData; // input JSON
    private JsonNode outputData; // result JSON (if any)

    private String errorInfo;

    public String getTransformationId() {
        return transformationId;
    }

    public void setTransformationId(String transformationId) {
        this.transformationId = transformationId;
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

    public Boolean getValidExecution() {
        return validExecution;
    }

    public void setValidExecution(Boolean validExecution) {
        this.validExecution = validExecution;
    }

    public JsonNode getSourceData() {
        return sourceData;
    }

    public void setSourceData(JsonNode sourceData) {
        this.sourceData = sourceData;
    }

    public JsonNode getOutputData() {
        return outputData;
    }

    public void setOutputData(JsonNode outputData) {
        this.outputData = outputData;
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }

}