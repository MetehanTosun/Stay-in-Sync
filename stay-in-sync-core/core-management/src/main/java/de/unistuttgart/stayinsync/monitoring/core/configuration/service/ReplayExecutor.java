package de.unistuttgart.stayinsync.monitoring.core.configuration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oracle.truffle.api.debug.DebugValue;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.graalvm.polyglot.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Executes a user-provided JavaScript transformation in a sandboxed GraalJS
 * context
 * for replay/debugging purposes.
 * <p>
 * Responsibilities:
 * <ul>
 * <li>Evaluate a {@code transform()} function inside a restricted GraalVM
 * {@link Context}.</li>
 * <li>Capture local variables at a debugging breakpoint on both success and
 * error.</li>
 * <li>Return the transformation result, captured variables, and any error
 * information.</li>
 * </ul>
 * The replay path avoids host access and native access, sets a statement limit
 * as an
 * execution guard, and injects only the minimal globals needed (e.g.
 * {@code source},
 * a no-op {@code stayinsync.log}, and a {@code __capture} helper) to ensure
 * safety.
 * </p>
 *
 * <p>
 * This class is part of the Snapshot/Replay subsystem: it re-runs
 * transformation code
 * using snapshot input to reproduce issues users observed during normal
 * execution.
 * </p>
 *
 * @author Mohammed-Ammar Hassnou
 */
@ApplicationScoped
public class ReplayExecutor {

    /**
     * Result of a replay execution.
     *
     * @param outputData the deep-converted value returned by {@code transform()}
     *                   when execution succeeds; {@code null} on error
     * @param variables  a map of variable names to their deep-converted values
     *                   captured at the debugger breakpoint
     * @param errorInfo  non-null error message (e.g., stack trace) if execution
     *                   failed; {@code null} otherwise
     */
    public record Result(
            Object outputData, // Whatever transform() returned, deep-converted
            Map<String, Object> variables, // Captured variable states from DebugScope
            String errorInfo // Non-null if an exception happened
    ) {
    }

    // Shared GraalVM engine instance; reused for performance across contexts.
    private final Engine graalEngine;
    // Jackson mapper for (de)serializing the source JSON and pretty debugging
    // output.
    private final ObjectMapper om;

    /**
     * Construct a new replay executor.
     *
     * @param om          Jackson {@link ObjectMapper} used to serialize input into
     *                    the JS context
     * @param graalEngine pre-configured GraalVM {@link Engine} used to build
     *                    per-run contexts
     */
    @Inject
    public ReplayExecutor(ObjectMapper om, Engine graalEngine) {
        this.graalEngine = graalEngine;
        this.om = om;
    }

    /**
     * Execute the provided JavaScript code by invoking a global {@code transform()}
     * function
     * within a sandboxed GraalJS context. Captures local variables on success or
     * error and
     * returns them alongside the output or error information.
     *
     * @param scriptName     optional logical name for the script (used as sourceURL
     *                       for stack traces)
     * @param javascriptCode the user-provided JavaScript containing a
     *                       {@code transform()} function
     * @param sourceData     JSON tree that will be exposed in the JS global as
     *                       {@code source}
     * @return a {@link Result} aggregating the transformation output, captured
     *         variables, and error info
     */
    public Result execute(String scriptName, String javascriptCode, JsonNode sourceData) {
        // Ensure a top-level 'source' object exists so scripts can safely read from
        // source.*
        ObjectNode a = sourceData.withObject("source");

        System.out.println(generatedSdkCode);

        // System.out.println("DEBUG: Executing replay with sourceData: " +
        // sourceData.toPrettyString());

        // Will collect variables captured from the JS context after execution.
        final Map<String, Object> capturedVars = new LinkedHashMap<>();
        // Provide a stable sourceURL to improve stack traces and debugging in GraalJS.
        final String SOURCE_URL = scriptName == null ? "replay.js" : scriptName;

        // Remove injection of debugger; inside transform()
        // javascriptCode = javascriptCode.replaceFirst(
        // "(?m)(function\\s+transform\\s*\\(\\s*\\)\\s*\\{)",
        // "$1 debugger;");

        // System.out.println(javascriptCode);

        // Wrap user code to call transform() and set debugger breakpoints for success
        // and error.
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

        // Build a restricted JS context: no host access, no native, statement limit as
        // guard.
        try (Context context = Context.newBuilder("js")
                .engine(graalEngine)
                .allowHostAccess(HostAccess.NONE) // no host access for safety
                .allowCreateThread(false)
                // IO is allowed here; adjust as needed depending on replay security policy.
                .allowIO(true)
                .allowNativeAccess(false)
                .resourceLimits(
                        ResourceLimits.newBuilder()
                                .statementLimit(1_000_000L, src -> true) // execution guard
                                .build())
                .build()) {

            // Provide a minimal 'stayinsync.log' to satisfy scripts that log during
            // execution.
            context.eval("js",
                    "var stayinsync = (typeof stayinsync !== 'undefined') ? stayinsync : {};\n" +
                            "if (typeof stayinsync.log !== 'function') {\n" +
                            "  stayinsync.log = function(msg, level) { /* no-op */ };\n" +
                            "}\n");

            // Define a global capture helper to collect locals into a JS object during
            // replay.
            context.eval("js",
                    "if (typeof globalThis.__capturedLocals === 'undefined') { globalThis.__capturedLocals = {}; }\n" +
                            "function __capture(name, value) { globalThis.__capturedLocals[name] = value; }\n");

            // Serialize the provided source data into the JS global scope.
            String sourceJson;
            try {
                sourceJson = om.writeValueAsString(sourceData);
            } catch (JsonProcessingException e) {
                return new Result(null, capturedVars, "JSON serialization error: " + e.getMessage());
            }

            var bindings = context.getBindings("js");
            bindings.putMember("sourceJson", sourceJson);
            context.eval("js", "globalThis.source = (JSON.parse(sourceJson)).source");
            // 2) bind the SDK string
            if (generatedSdkCode == null)
                generatedSdkCode = "";
            bindings.putMember("sdkCode", generatedSdkCode);

            // Load user code + wrapper; this defines the __replayEntry function.
            // 3) execute the SDK in the GLOBAL scope so `var targets` becomes global
            // Use *indirect eval* to ensure top-level `var targets = {}` lands on
            // globalThis.
            context.eval("js", "(0, eval)(sdkCode)");

            // 4) (optional) harden: if SDK was empty, provide a stub to avoid
            // ReferenceError
            context.eval("js",
                    "if (typeof globalThis.targets === 'undefined') {" +
                            "  globalThis.targets = {" +
                            "    synchronizeProducts: { defineUpsert: function(){ return { " +
                            "      usingCheck(){return this}, usingCreate(){return this}, usingUpdate(){return this}, build(){return this} "
                            +
                            "    }} }" +
                            "  };" +
                            "}");
            // Remove usage of Truffle Debugger session since __capture replaces it
            // Load user code + wrapper (defines __replayEntry)
            Value entry = context.eval("js", wrapped);

            // Invoke the wrapper, which calls transform() and returns a status object.
            Value res = entry.execute();

            Object output = null;
            String errorInfo = null;

            // On success, deep-convert the returned value into plain Java types.
            if (res.hasMembers() && res.getMember("ok").asBoolean()) {
                output = convertPolyglotValue(res.getMember("value"));
            } else {
                // On error, extract a stringified error/stack from the wrapper's result.
                errorInfo = res.hasMembers() && res.getMemberKeys().contains("error")
                        ? res.getMember("error").asString()
                        : "Unknown script error";
            }

            // After execution, fetch captured locals from the JS global and convert them.
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

            // Summarize replay outcome and return to caller.
            return new Result(output, capturedVars, errorInfo);

        } catch (PolyglotException pe) {
            // Catch failures outside JS try/catch (e.g., resource limits) and surface
            // message.
            return new Result(null, capturedVars, "PolyglotException: " + pe.getMessage());
        }
    }

    /**
     * Recursively convert GraalVM {@link Value} and Truffle {@link DebugValue}
     * instances
     * into plain Java primitives, Lists, and Maps suitable for transport/logging.
     * Non-convertible values fall back to {@code toString()} or a descriptive
     * placeholder.
     *
     * @param val a Graal {@link Value}, a {@link DebugValue}, or a plain Java
     *            object
     * @return a deep-converted Java representation
     */
    private static Object convertPolyglotValue(Object val) {
        // Handle Polyglot Value: convert primitives, arrays, and member objects.
        if (val instanceof Value v) {
            // Primitive conversion.
            if (v.isNull())
                return null;
            // Primitive conversion.
            if (v.isBoolean())
                return v.asBoolean();
            // Primitive conversion.
            if (v.isString())
                return v.asString();
            // Primitive conversion.
            if (v.fitsInInt())
                return v.asInt();
            // Primitive conversion.
            if (v.fitsInLong())
                return v.asLong();
            // Primitive conversion.
            if (v.fitsInDouble())
                return v.asDouble();

            // Convert JS arrays to Java Lists (deep-converted).
            if (v.hasArrayElements()) {
                int size = (int) v.getArraySize();
                java.util.List<Object> list = new java.util.ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(convertPolyglotValue(v.getArrayElement(i)));
                }
                return list;
            }
            // Convert JS objects to Java Maps (deep-converted).
            if (v.hasMembers()) {
                java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                for (String key : v.getMemberKeys()) {
                    map.put(key, convertPolyglotValue(v.getMember(key)));
                }
                return map;
            }
            return v.toString();

            // Handle Truffle DebugValue: capture display string and recursively convert
            // children.
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
        // Fallback: already a plain Java object.
        return val;
    }
}
