package de.unistuttgart.stayinsync.scriptengine;

import org.graalvm.polyglot.HostAccess;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScriptApi {

    private final Object inputData;
    private Object outputData;

    // TODO: Implement a proper way to assign ExecutionContext
    /*private final ExecutionContext executionContext;*/

    public ScriptApi(Object inputData/*, ExecutionContext executionContext*/) {
        // TODO: Check requirements for possible immutability or deepcopy of inputData for security reasons
        this.inputData = inputData;
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
        System.out.println("Script Log [" + logLevel + "]: " + message);
    }
}
