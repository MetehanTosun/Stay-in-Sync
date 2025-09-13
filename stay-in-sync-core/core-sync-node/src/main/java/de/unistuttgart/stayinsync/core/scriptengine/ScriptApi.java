package de.unistuttgart.stayinsync.core.scriptengine;

import io.quarkus.logging.Log;
import org.graalvm.polyglot.HostAccess;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.util.Map;

/**
 * Provides an Application Programming Interface (API) for scripts executed within the GraalVM environment.
 * Instances of this class are made available to the script, allowing it to interact with the host Java application
 * in a controlled manner. Methods intended to be callable from the script are annotated with
 * {@link HostAccess.Export}.
 *
 * <p>This API allows scripts to:
 * <ul>
 *     <li>Access input data provided by the host application.</li>
 *     <li>Set output data to be consumed by the host application.</li>
 *     <li>Log messages using the host's logging infrastructure, with contextual information like a job ID.</li>
 * </ul>
 * </p>
 *
 * <p>The {@code TODO} comments indicate areas for future development, such as handling execution context,
 * ensuring data immutability for security, and enhancing logging capabilities.</p>
 *
 * @author Maximilian Peresunchak
 * @since 1.0
 */
public class ScriptApi {
    private final Object inputData;
    private final String jobId;

    // TODO: Implement a proper way to assign ExecutionContext
    /*private final ExecutionContext executionContext;*/

    /**
     * Constructs a new {@code ScriptApi} instance.
     *
     * @param inputData The data to be made available to the script via {@link #getInput()}.
     *                                   TODO: Check requirements for possible immutability or deep copy of inputData for security reasons.
     * @param jobId     The identifier for the current job, used for contextual information like logging.
     *                  // @param executionContext The execution context associated with this script run.
     *                  //                       TODO: Implement and integrate ExecutionContext.
     */
    public ScriptApi(Object inputData, String jobId/*, ExecutionContext executionContext*/) {
        this.inputData = inputData;
        this.jobId = jobId;
        /*this.executionContext = executionContext;*/
    }

    /**
     * Retrieves the input data for the script.
     * This method is exposed to the script environment via the {@link HostAccess.Export} annotation.
     * <p>
     * If the top-level {@code inputData} is a {@link Map}, this method creates a defensive copy.
     * If any values within this top-level map are themselves {@link Map}s, those nested maps are also
     * defensively copied using {@link Map#copyOf(Map)}. This helps to prevent the script from
     * modifying the original input data structures in the host application, providing a degree of isolation.
     * Other types of data are returned as is.
     * </p>
     *
     * @return The input data for the script. If the input was a Map, it returns a copy
     * (with nested Maps also copied). Otherwise, returns the original input object.
     */
    @HostAccess.Export
    public Object getInput() {
        if (inputData instanceof Map) {
            Map<String, Object> namespacedData = (Map<String, Object>) inputData;
            for (Map.Entry<String, Object> entry : namespacedData.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    namespacedData.put(entry.getKey(), Map.copyOf((Map<String, Object>) entry.getValue()));
                } else {
                    namespacedData.put(entry.getKey(), entry.getValue());
                }
            }

            return Map.copyOf(namespacedData);
        } else {
            return inputData;
        }
    }

    /**
     * Allows the script to log a message using the host application's logging infrastructure.
     * This method is exposed to the script environment via the {@link HostAccess.Export} annotation.
     * The {@code jobId} is automatically added to the Mapped Diagnostic Context (MDC) for the duration
     * of the log call, enriching log entries with contextual information.
     * <p>
     * TODO: Further integration with the logging environment and discussion on metrics generation are pending.
     * TODO: Add scriptID logging as MDC.
     * </p>
     *
     * @param message  The message string to be logged.
     * @param logLevel A string representing the desired log level (e.g., "INFO", "WARN", "ERROR", "DEBUG", "TRACE").
     *                 If null, invalid, or an unrecognized value is provided, it defaults to "INFO".
     *                 The level is case-insensitive.
     */
    @HostAccess.Export
    public void log(String message, String logLevel) {
        MDC.put("jobId", jobId);
        try {
            Logger.Level level = Logger.Level.INFO;
            if (logLevel != null && !logLevel.trim().isEmpty()) {
                try {
                    level = Logger.Level.valueOf(logLevel.toUpperCase());
                } catch (IllegalArgumentException e) {
                    Log.warnf("Invalid log level '%s' provided by script. Defaulting to INFO.", logLevel);
                }
            }
            Log.log(level, message);
        } finally {
            MDC.remove("jobId");
        }
    }
}
