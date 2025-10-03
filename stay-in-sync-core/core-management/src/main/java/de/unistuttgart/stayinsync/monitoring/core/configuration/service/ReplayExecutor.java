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
import com.oracle.truffle.api.debug.DebugValue;

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

        // Remove injection of debugger; inside transform()
        // javascriptCode = javascriptCode.replaceFirst(
        // "(?m)(function\\s+transform\\s*\\(\\s*\\)\\s*\\{)",
        // "$1 debugger;");

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

            // Inject __capture helper function into JS context
            context.eval("js",
                    "if (typeof globalThis.__capturedLocals === 'undefined') { globalThis.__capturedLocals = {}; }\n" +
                            "function __capture(name, value) { globalThis.__capturedLocals[name] = value; }\n");

            // Serialize the "source" object into JS context
            String sourceJson;
            try {
                sourceJson = om.writeValueAsString(sourceData);
            } catch (JsonProcessingException e) {
                return new Result(null, capturedVars, "JSON serialization error: " + e.getMessage());
            }
            context.eval("js", "var source = " + sourceJson + ";");

            // Remove usage of Truffle Debugger session since __capture replaces it
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

            // After execution, read globalThis.__capturedLocals and convert to capturedVars
            Value capturedLocalsValue = context.eval("js", "globalThis.__capturedLocals");
            if (capturedLocalsValue != null && capturedLocalsValue.hasMembers()) {
                capturedVars.clear();
                for (String key : capturedLocalsValue.getMemberKeys()) {
                    try {
                        capturedVars.put(key, convertPolyglotValue(capturedLocalsValue.getMember(key)));
                    } catch (Exception ignore) {
                        // Some values may not be convertible
                    }
                }
            }

            return new Result(output, capturedVars, errorInfo);

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
