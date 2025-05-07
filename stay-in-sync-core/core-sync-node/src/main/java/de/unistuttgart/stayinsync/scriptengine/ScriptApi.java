package de.unistuttgart.stayinsync.scriptengine;

import org.graalvm.polyglot.HostAccess;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScriptApi {

    private final Map<String, Object> allAvailableNamespaces;

    private final Object inputData;
    private Object outputData;

    // TODO: Implement a proper way to assign ExecutionContext
    /*private final ExecutionContext executionContext;*/

    public ScriptApi(Object inputData, Map<String, Object> allNamespaces/*, ExecutionContext executionContext*/) {
        // TODO: Check requirements for possible immutability or deepcopy of inputData for security reasons
        this.inputData = inputData;
        this.allAvailableNamespaces = allNamespaces;
        /*this.executionContext = executionContext;*/
    }

    @HostAccess.Export
    public Object getInput() {
        return inputData;
    }

    // Expose immutable Map for the requested namespace
    @HostAccess.Export
    public Object getNamespace(String namespaceName){
        Map<String, Object> namespaceData = (Map<String, Object>) allAvailableNamespaces.get(namespaceName);
        if(namespaceData != null){
            return Map.copyOf(namespaceData);
        }
        return null;
    }

    // Expose all namespaces as immutable Map
    @HostAccess.Export
    public Object getAllNamespaces(){
        Map<String, Object> unmodifiableOuter = new HashMap<>();
        for (Map.Entry<String, Object> entry : allAvailableNamespaces.entrySet()) {
            if (entry.getValue() instanceof Map) {
                unmodifiableOuter.put(entry.getKey(), Map.copyOf((Map<String, Object>) entry.getValue()));
            } else {
                unmodifiableOuter.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(unmodifiableOuter);
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
