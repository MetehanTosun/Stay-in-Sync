package de.unistuttgart.stayinsync.core.monitoring.error;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.monitoring.ScriptEngineErrorEvent;
import io.quarkus.logging.Log;

/**
 * Utility class for monitoring-related actions such as logging structured events
 * in a format suitable for ingestion by ELK.
 * This class provides helper methods to serialize and log error events.
 */
public class MonitoringUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Logs a ScriptEngineErrorEvent to the structured logging pipeline.
     * @param event the ScriptEngineErrorEvent containing job and error metadata
     */
    public static void logScriptEngineError(ScriptEngineErrorEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            Log.error(json);

        } catch (JsonProcessingException e) {
            Log.error("Failed to serialize ScriptEngineErrorEvent", e);
        }
    }

    /**
     * Convenience method to construct and log a ScriptEngineErrorEvent from individual parameters.
     * Useful when you want to avoid manually creating the event object.
     *
     * @param jobId     the unique job identifier
     * @param scriptId  the unique script identifier
     * @param errorType a short string indicating the error category
     * @param message   the detailed error message
     */
    public static void logScriptEngineError(String jobId, String scriptId, String errorType, String message) {
        ScriptEngineErrorEvent event = new ScriptEngineErrorEvent(jobId, scriptId, errorType, message);
        logScriptEngineError(event);
    }

    /**
     * Logs a generic unexpected error with stack trace context.
     * This method ensures even unknown or system-level exceptions are pushed
     * to your monitoring pipeline in a structured format.
     * @param jobId     the job identifier where the error occurred
     * @param scriptId  the script identifier
     * @param throwable the caught exception
     */
    public static void logUnexpectedScriptEngineException(String jobId, String scriptId, Throwable throwable) {
        String errorType = "UNEXPECTED_ERROR";
        String message = throwable.getClass().getSimpleName() + ": " + throwable.getMessage();

        ScriptEngineErrorEvent event = new ScriptEngineErrorEvent(jobId, scriptId, errorType, message);
        logScriptEngineError(event);
    }
}
