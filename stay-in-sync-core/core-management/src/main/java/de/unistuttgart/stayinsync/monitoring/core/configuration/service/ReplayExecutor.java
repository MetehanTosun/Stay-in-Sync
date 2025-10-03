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
 * Runs a transformation script in a sandboxed GraalJS context.
 *
 * - Executes a user-provided transform() function
 * - Captures local variables from the transform() stack frame
 * (on both success and error) via Truffle Debug API
 * - Returns transform() result, captured variables, and error info
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
        // Ensure "source" object always exists in input
        ObjectNode a = sourceData.withObject("source");

        System.out.println("DEBUG: Executing replay with sourceData: " + sourceData.toPrettyString());

        final Map<String, Object> capturedVars = new LinkedHashMap<>();
        final String SOURCE_URL = scriptName == null ? "replay.js" : scriptName;

        // Inject debugger at start of transform()
        javascriptCode = javascriptCode.replaceFirst(
                "(?m)(function\\s+transform\\s*\\(\\s*\\)\\s*\\{)",
                "$1 debugger;");

        System.out.println(javascriptCode);

        // Wrap user code with driver that calls transform()
        // We inject a debugger; statement both on success and error
        // so we can snapshot variables in either case
        String wrapped = """
                // --- user code start ---
                %s

                // --- driver ---
                (function __replayEntry() {
                    try {
                        var result = transform();
                        debugger; // snapshot on success
                        return { ok: true, value: result };
                    } catch (e) {
                        debugger; // snapshot on error
                        return { ok: false, error: String(e && e.stack ? e.stack : e) };
                    }
                })
                //# sourceURL=%s
                """.formatted(javascriptCode, SOURCE_URL);

        try (Context context = Context.newBuilder("js")
                .engine(graalEngine)
                .allowHostAccess(HostAccess.NONE) // no host access for safety
                .allowCreateThread(false)
                .allowIO(true)
                .allowNativeAccess(false)
                .resourceLimits(
                        ResourceLimits.newBuilder()
                                .statementLimit(1_000_000L, src -> true) // execution guard
                                .build())
                .build()) {

            // Provide a minimal global `stayinsync.log` (no-op in replay mode)
            context.eval("js",
                    "var stayinsync = (typeof stayinsync !== 'undefined') ? stayinsync : {};\n" +
                            "if (typeof stayinsync.log !== 'function') {\n" +
                            "  stayinsync.log = function(msg, level) { /* no-op */ };\n" +
                            "}\n");

            // Serialize the "source" object into JS context
            String sourceJson;
            try {
                sourceJson = om.writeValueAsString(sourceData);
            } catch (JsonProcessingException e) {
                return new Result(null, capturedVars, "JSON serialization error: " + e.getMessage());
            }
            context.eval("js", "var source = " + sourceJson + ";");

            // Start a debugger session that listens for "debugger;" statements
            Debugger debugger = Debugger.find(graalEngine);
            try (DebuggerSession session = debugger.startSession((SuspendedEvent ev) -> {

                // Look through all stack frames to find the user’s transform() frame
                for (DebugStackFrame frame : ev.getStackFrames()) {
                    if ("transform".equals(frame.getName())) {
                        DebugScope scope = frame.getScope();

                        // Walk scopes inside transform() to capture locals
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
                        break; // only need transform() frame
                    }
                }

                // Continue execution after snapshot
                ev.prepareContinue();
            })) {
                // Load user code + wrapper (defines __replayEntry)
                Value entry = context.eval("js", wrapped);

                // Call __replayEntry, which in turn calls transform()
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
            // Top-level Polyglot error (not caught by JS try/catch)
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
