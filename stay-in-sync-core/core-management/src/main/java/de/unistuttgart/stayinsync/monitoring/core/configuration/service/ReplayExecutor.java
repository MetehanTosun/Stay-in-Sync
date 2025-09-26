package de.unistuttgart.stayinsync.monitoring.core.configuration.service;

import java.util.LinkedHashMap;
import java.util.Map;

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

    private final ObjectMapper om;

    public ReplayExecutor(ObjectMapper om) {
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

        // 1) Prepare a place to receive variables at the moment of suspension.
        final Map<String, Object> capturedVars = new LinkedHashMap<>();
        final String SOURCE_URL = scriptName == null ? "replay.js" : scriptName;

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
                (function __replayEntry(__replaySource) {
                  try {
                    return { ok: true, value: transform(__replaySource) };
                  } catch (e) {
                    // Force suspension for Truffle Debugger:
                    debugger;
                    return { ok: false, error: String(e && e.stack ? e.stack : e) };
                  }
                })
                //# sourceURL=%s
                """.formatted(javascriptCode, SOURCE_URL);

        // 3) Build Engine + Context. Keep it sandboxed and bounded.
        Engine engine = Engine.newBuilder()
                // You can enable/disable compilation, stack traces, etc. here if desired
                .option("engine.WarnInterpreterOnly", "false")
                .build();

        Context context = Context.newBuilder("js")
                .engine(engine)
                .allowHostAccess(HostAccess.NONE)
                .allowCreateThread(false)
                .allowIO(false)
                .allowNativeAccess(false)
                .resourceLimits(
                        ResourceLimits.newBuilder()
                                .statementLimit(1_000_000L, src -> true) // apply to all sources
                                .build())
                .build();

        // Provide a minimal global `stayinsync.log` so scripts that call it do not
        // crash during replay.
        context.eval("js",
                "var stayinsync = (typeof stayinsync !== 'undefined') ? stayinsync : {};\n" +
                        "if (typeof stayinsync.log !== 'function') { stayinsync.log = function(msg, level) { /* no-op in replay */ }; }\n");

        // 4) Start a Debugger session. The SuspendedEvent callback fires when we hit
        // `debugger;`.
        Debugger debugger = Debugger.find(engine);
        try (DebuggerSession session = debugger.startSession((SuspendedEvent ev) -> {
            DebugStackFrame frame = ev.getTopStackFrame(); // <-- use stack frame
            DebugScope scope = frame.getScope(); // <-- then get the scope

            for (DebugScope s = scope; s != null; s = s.getParent()) {
                for (DebugValue v : s.getDeclaredValues()) {
                    try {
                        capturedVars.putIfAbsent(v.getName(), v.as(Object.class));
                    } catch (Exception ignore) {
                        /* some values may not be convertible */ }
                }
            }

            // We only wanted to sample; resume right away.
            ev.prepareContinue();
        })) {

            // 5) Evaluate the wrapped code to get our entry function.
            Value entry = context.eval("js", wrapped);

            // 6) Convert the JSON input into a JS value (Graal maps Maps/Lists/POJOs
            // automatically).
            Object sourcePojo = om.convertValue(sourceData, Object.class);

            // Also bind `source` globally so scripts that read a global `source` work, too.
            context.getBindings("js").putMember("source", sourcePojo);

            // 7) Call the driver. It returns a small record { ok: boolean, value? or error?
            // }.
            Value res = entry.execute(sourcePojo);

            Object output = null;
            String errorInfo = null;

            if (res.hasMembers() && res.getMember("ok").asBoolean()) {
                output = res.getMember("value").as(Object.class);
            } else {
                // If we got here because of `debugger;`, our SuspendedEvent handler already
                // collected vars.
                errorInfo = res.hasMembers() && res.getMemberKeys().contains("error")
                        ? res.getMember("error").asString()
                        : "Unknown script error";
            }

            return new Result(output, capturedVars, errorInfo);

        } catch (PolyglotException pe) {
            // A hard engine/context failure (or resource limit) — not caught by our driver.
            return new Result(null, capturedVars, "PolyglotException: " + pe.getMessage());
        } finally {
            context.close(true);
            engine.close();
        }
    }
}