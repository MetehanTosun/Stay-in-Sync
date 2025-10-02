package de.unistuttgart.stayinsync.monitoring.core.configuration.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.ResourceLimits;
import org.graalvm.polyglot.Value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oracle.truffle.api.debug.DebugScope;
import com.oracle.truffle.api.debug.DebugStackFrame;
import com.oracle.truffle.api.debug.DebugValue;
import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.debug.DebuggerSession;
import com.oracle.truffle.api.debug.SuspendedEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Runs a transformation script in a sandboxed GraalJS context, captures
 * variables via Truffle Debug API when suspended (on error or injected
 * breakpoint),
 * and returns outputData + captured variable states.
 *
 * Contract: The user script defines a function:
 * function transform() { ... return <any>; }
 * and expects "source" to exist in the global scope.
 */
@ApplicationScoped
public class ReplayExecutor {

    public record Result(
            Object outputData, // Whatever transform() returned, deep-converted
            Map<String, Object> variables, // Captured variable states from DebugScope
            String errorInfo // Non-null if an exception happened
    ) {
    }

    private final Engine graalEngine;
    private final ObjectMapper om;

    @Inject
    public ReplayExecutor(ObjectMapper om, Engine graalEngine) {
        this.graalEngine = graalEngine;
        this.om = om;
    }

    public Result execute(String scriptName, String javascriptCode, JsonNode sourceData) {
        ObjectNode a = sourceData.withObject("source"); // ensure "source" node exists

        System.out.println("DEBUG: Executing replay with sourceData: " + sourceData.toPrettyString());

        final Map<String, Object> capturedVars = new LinkedHashMap<>();
        final String SOURCE_URL = scriptName == null ? "replay.js" : scriptName;

        // Wrap user code + driver
        String wrapped = """
                // --- user code start ---
                %s

                // --- driver ---
                (function __replayEntry() {
                    try {
                        return { ok: true, value: transform() };
                    } catch (e) {
                        debugger;
                        return { ok: false, error: String(e && e.stack ? e.stack : e) };
                    }
                })
                //# sourceURL=%s
                """.formatted(javascriptCode, SOURCE_URL);

        try (Context context = Context.newBuilder("js")
                .engine(graalEngine)
                .allowHostAccess(HostAccess.NONE)
                .allowCreateThread(false)
                .allowIO(true)
                .allowNativeAccess(false)
                .resourceLimits(
                        ResourceLimits.newBuilder()
                                .statementLimit(1_000_000L, src -> true)
                                .build())
                .build()) {

            // Provide a minimal global `stayinsync.log`
            context.eval("js",
                    "var stayinsync = (typeof stayinsync !== 'undefined') ? stayinsync : {};\n" +
                            "if (typeof stayinsync.log !== 'function') { stayinsync.log = function(msg, level) { /* no-op in replay */ }; }\n");

            // Bind "source" globally as a native JS object
            String sourceJson;
            try {
                sourceJson = om.writeValueAsString(sourceData);
            } catch (JsonProcessingException e) {
                return new Result(null, capturedVars, "JSON serialization error: " + e.getMessage());
            }
            context.eval("js", "var source = " + sourceJson + ";");

            // Debugger session to capture vars on suspend
            Debugger debugger = Debugger.find(graalEngine);
            try (DebuggerSession session = debugger.startSession((SuspendedEvent ev) -> {
                DebugStackFrame frame = ev.getTopStackFrame();
                if (frame == null)
                    return;
                DebugScope scope = frame.getScope();

                for (DebugScope s = scope; s != null; s = s.getParent()) {
                    for (DebugValue v : s.getDeclaredValues()) {
                        if (!v.isInternal()) {
                            try {
                                capturedVars.putIfAbsent(v.getName(), convertPolyglotValue(v));
                            } catch (Exception ignore) {
                                // Some values may not be convertible
                            }
                        }
                    }
                }
                ev.prepareContinue();
            })) {
                // Evaluate wrapped script (defines __replayEntry)
                Value entry = context.eval("js", wrapped);

                // Execute driver, which calls transform() using global "source"
                Value res = entry.execute();

                Object output = null;
                String errorInfo = null;

                if (res.hasMembers() && res.getMember("ok").asBoolean()) {
                    output = convertPolyglotValue(res.getMember("value"));
                } else {
                    errorInfo = res.hasMembers() && res.getMemberKeys().contains("error")
                            ? res.getMember("error").asString()
                            : "Unknown script error";
                }
                return new Result(output, capturedVars, errorInfo);
            }
        } catch (PolyglotException pe) {
            return new Result(null, capturedVars, "PolyglotException: " + pe.getMessage());
        }
    }

    /**
     * Deep-convert GraalVM Values and DebugValues into plain Java objects.
     */
    private static Object convertPolyglotValue(Object val) {
        if (val instanceof Value v) {
            if (v.isNull())
                return null;
            if (v.isBoolean())
                return v.asBoolean();
            if (v.isString())
                return v.asString();
            if (v.fitsInInt())
                return v.asInt();
            if (v.fitsInLong())
                return v.asLong();
            if (v.fitsInDouble())
                return v.asDouble();
            if (v.hasArrayElements()) {
                int size = (int) v.getArraySize();
                java.util.List<Object> list = new java.util.ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(convertPolyglotValue(v.getArrayElement(i)));
                }
                return list;
            }
            if (v.hasMembers()) {
                java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                for (String key : v.getMemberKeys()) {
                    map.put(key, convertPolyglotValue(v.getMember(key)));
                }
                return map;
            }
            return v.toString();
        } else if (val instanceof DebugValue dv) {
            try {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("_value", dv.toDisplayString());
                if (dv.getProperties() != null) {
                    for (DebugValue child : dv.getProperties()) {
                        map.put(child.getName(), convertPolyglotValue(child));
                    }
                }
                return map;
            } catch (Exception e) {
                return "[Unconvertible DebugValue: " + dv.getName() + "]";
            }
        }
        return val;
    }
}
