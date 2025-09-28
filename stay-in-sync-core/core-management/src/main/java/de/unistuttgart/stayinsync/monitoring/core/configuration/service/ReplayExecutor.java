package de.unistuttgart.stayinsync.monitoring.core.configuration.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.inject.Inject;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.ResourceLimits;
import org.graalvm.polyglot.Value;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.truffle.api.debug.DebugScope;
import com.oracle.truffle.api.debug.DebugStackFrame;
import com.oracle.truffle.api.debug.DebugValue;
import com.oracle.truffle.api.debug.Debugger;
import com.oracle.truffle.api.debug.DebuggerSession;
import com.oracle.truffle.api.debug.SuspendedEvent;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Runs a transformation script in a sandboxed GraalJS context, captures
 * variables
 * via Truffle Debug API when suspended (on error or injected breakpoint),
 * and returns outputData + captured variable states.
 *
 * Contract: The user script defines a function:
 * function transform(source) { ... return <any>; }
 */
@ApplicationScoped
public class ReplayExecutor {

    public record Result(
            Object outputData, // Whatever transform() returned (will be JSON-serializable by Jackson on our
                               // side)
            Map<String, Object> variables, // Captured variable states from DebugScope on suspend
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

    /**
     * Execute the given TypeScript->JavaScript (or plain JS) code with the provided
     * sourceData.
     * We inject a try/catch that triggers a `debugger;` so the Truffle Debugger
     * suspends and lets
     * us read variable states via DebugScope before the exception escapes.
     *
     * @param scriptName     purely for nicer stack traces (sourceURL)
     * @param javascriptCode JS code that declares: function transform(source) { ...
     *                       }
     * @param sourceData     JSON for the 'source' argument
     */
    public Result execute(String scriptName, String javascriptCode, JsonNode sourceData) {
        System.out.println("DEBUG: Executing replay with sourceData: " + sourceData.toPrettyString());
        // 1) Prepare a place to receive variables at the moment of suspension.
        final Map<String, Object> capturedVars = new LinkedHashMap<>();
        final String SOURCE_URL = scriptName == null ? "replay.js" : scriptName;

        String debugJavascript = """
    function transform() {
        console.log('--- GUEST TRUTH --- The global "source" object is:');
        console.log(JSON.stringify(source));
    """ + javascriptCode.substring(javascriptCode.indexOf("{") + 1);

        /*
         * 2) Wrap the user code so we can suspend at the exact failure point.
         * - We keep the user-provided function transform(source) as-is.
         * - We add a small driver that calls it inside try/catch.
         * - On exception, we execute `debugger;` to force a Truffle suspension.
         *
         * NOTE: This lets us capture variable states without doing AST rewriting or
         * heavy instrumentation.
         * We still get a consistent suspension point.
         */
        String wrapped = """
                // --- user code start ---
                %s
                
                // --- driver ---
                (function __replayEntry(__sourceDataArgument) {
                try {
                    var source = __sourceDataArgument;
                    \s
                    return { ok: true, value: transform() };
                } catch (e) {
                     debugger;
                     return { ok: false, error: String(e && e.stack ? e.stack : e) };
                 }
                })
                //# sourceURL=%s
                """.formatted(debugJavascript, SOURCE_URL);

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

            // Find the Debugger associated with the shared Engine
            Debugger debugger = Debugger.find(graalEngine);
            try (DebuggerSession session = debugger.startSession((SuspendedEvent ev) -> {
                DebugStackFrame frame = ev.getTopStackFrame();
                if (frame == null) return;
                DebugScope scope = frame.getScope();

                for (DebugScope s = scope; s != null; s = s.getParent()) {
                    for (DebugValue v : s.getDeclaredValues()) {
                        if (!v.isInternal()) {
                            try {
                                capturedVars.putIfAbsent(v.getName(), v.as(Object.class));
                            } catch (Exception ignore) { /* some values may not be convertible */ }
                        }
                    }
                }
                ev.prepareContinue();
            })) {
                Value entry = context.eval("js", wrapped);
                JsonNode dataToBind = sourceData.has("source") ? sourceData.get("source") : sourceData;

                // 2. Convert it to a standard Map using a more explicit TypeReference.
                //    This ensures there are no Jackson-specific object types.
                TypeReference<Map<String, Object>> mapTypeRef = new TypeReference<>() {};
                Map<String, Object> sourceAsMap = om.convertValue(dataToBind, mapTypeRef);

                // 3. Execute the entry function, PASSING THE MAP AS AN ARGUMENT.
                //    We no longer use putMember. We are directly invoking the function with data.
                Value res = entry.execute(sourceAsMap);

                Object output = null;
                String errorInfo = null;

                if (res.hasMembers() && res.getMember("ok").asBoolean()) {
                    output = res.getMember("value").as(Object.class);
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
}