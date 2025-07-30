package de.unistuttgart.stayinsync.monitoring;

import java.time.Instant;

public class ScriptEngineErrorEvent {

    private String jobId;
    private String scriptId;
    private String errorType;
    private String message;
    private long timestamp;

    public ScriptEngineErrorEvent() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    public ScriptEngineErrorEvent(String jobId, String scriptId, String errorType, String message) {
        this.jobId = jobId;
        this.scriptId = scriptId;
        this.errorType = errorType;
        this.message = message;
        this.timestamp = Instant.now().toEpochMilli();
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

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}