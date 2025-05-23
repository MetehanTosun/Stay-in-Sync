package de.unistuttgart.stayinsync.scriptengine;

import org.graalvm.polyglot.HostAccess;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.util.Map;

public class ScriptApi {

    private static final Logger SCRIPT_LOG = Logger.getLogger("ScriptExecutionLogger");

    private final Object inputData;
    private Object outputData;
    private final String jobId;

    // TODO: Implement a proper way to assign ExecutionContext
    /*private final ExecutionContext executionContext;*/

    public ScriptApi(Object inputData, String jobId/*, ExecutionContext executionContext*/) {
        // TODO: Check requirements for possible immutability or deepcopy of inputData for security reasons
        this.inputData = inputData;
        this.jobId = jobId;
        /*this.executionContext = executionContext;*/
    }

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

    @HostAccess.Export
    public void setOutput(Object outputData) {
        this.outputData = outputData;
    }

    public Object getOutputData() {
        return outputData;
    }

    // TODO: Integrate with logging environment and discuss metrics generation.
    @HostAccess.Export
    public void log(String message, String logLevel) {
        MDC.put("jobId", jobId);
        // TODO: Add scriptID logging as MDC
        try{
            Logger.Level level = Logger.Level.INFO;
            if(logLevel != null){
                try{
                    level = Logger.Level.valueOf(logLevel.toUpperCase());
                } catch (IllegalArgumentException e){
                    SCRIPT_LOG.warnf("Invalid log level '%s' provided by script. Defaulting to INFO.", logLevel);
                }
            }
            SCRIPT_LOG.log(level, message);
        } finally{
            MDC.remove("jobId");
            // TODO remove scriptId MDC
        }
    }
}
