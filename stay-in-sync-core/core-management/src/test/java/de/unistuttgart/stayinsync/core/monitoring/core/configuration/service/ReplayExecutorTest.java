package de.unistuttgart.stayinsync.core.monitoring.core.configuration.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.graalvm.polyglot.Engine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import de.unistuttgart.stayinsync.monitoring.core.configuration.service.ReplayExecutor;

/**
 * Unit tests for {@link ReplayExecutor}.
 * <p>
 * These tests exercise success and failure paths, variable capture behavior,
 * SDK injection, and resilience against JSON serialization failures.
 * </p>
 *
 * <p>
 * The tests use a real GraalVM {@link Engine} and a standard Jackson
 * {@link ObjectMapper}
 * to emulate the sandboxed execution as closely as possible without touching
 * production logic.
 * </p>
 *
 * @author Mohammed-Ammar Hassnou
 */
public class ReplayExecutorTest {

    private final Engine engine = Engine.newBuilder().build();
    private final ObjectMapper om = new ObjectMapper();

    private static ObjectNode makeSource(ObjectMapper om, Map<String, Object> kv) {
        ObjectNode root = om.createObjectNode();
        ObjectNode src = root.putObject("source");
        if (kv != null) {
            kv.forEach((k, v) -> src.putPOJO(k, v));
        }
        return root;
    }

    @Test
    @DisplayName("execute(): returns output and captures variables on success")
    void execute_success_returnsOutputAndCapturesVars() {
        // arrange
        ReplayExecutor sut = new ReplayExecutor(om, engine);
        String js = "function transform(){\n" +
                "  var x=1, y=2;\n" +
                "  __capture('sum', x+y);\n" +
                "  return { ok:true, total:x+y, sourceName: source.name };\n" +
                "}";
        JsonNode source = makeSource(om, Map.of("name", "Alice"));

        // act
        ReplayExecutor.Result res = sut.execute("test-success.js", js, source, "");

        // assert
        assertNull(res.errorInfo(), "Expected no errorInfo on success");
        assertNotNull(res.outputData(), "Expected non-null outputData");
        assertTrue(res.outputData() instanceof Map, "Output should be mapped to a Java Map");
        @SuppressWarnings("unchecked")
        Map<String, Object> out = (Map<String, Object>) res.outputData();
        assertEquals(3, ((Number) out.get("total")).intValue());
        assertEquals("Alice", out.get("sourceName"));
        assertEquals(3, ((Number) res.variables().get("sum")).intValue(), "Captured var 'sum' must be 3");
    }

    @Test
    @DisplayName("execute(): returns errorInfo and preserved captured vars on thrown error")
    void execute_error_returnsErrorInfoAndCapturedVars() {
        // arrange
        ReplayExecutor sut = new ReplayExecutor(om, engine);
        String js = "function transform(){\n" +
                "  var a = 42; __capture('a', a);\n" +
                "  throw new Error('fail');\n" +
                "}";
        JsonNode source = makeSource(om, Map.of());

        // act
        ReplayExecutor.Result res = sut.execute("test-error.js", js, source, "");

        // assert
        assertNull(res.outputData(), "Output must be null on error");
        assertNotNull(res.errorInfo(), "errorInfo must be present on error");
        assertTrue(res.errorInfo().toLowerCase().contains("fail"));
        assertEquals(42, ((Number) res.variables().get("a")).intValue(), "Captured var 'a' must be preserved");
    }

    @Test
    @DisplayName("execute(): supports generated SDK injection used by user code")
    void execute_sdkInjection_allowsUsingTargets() {
        // arrange
        ReplayExecutor sut = new ReplayExecutor(om, engine);
        String sdk = "var targets = { math: { add: function(a,b){ return a+b; } } };";
        String js = "function transform(){ return targets.math.add(5,7); }";
        JsonNode source = makeSource(om, Map.of());

        // act
        ReplayExecutor.Result res = sut.execute("test-sdk.js", js, source, sdk);

        // assert
        assertNull(res.errorInfo());
        assertEquals(12, ((Number) res.outputData()).intValue());
    }

    @Test
    @DisplayName("execute(): provides built-in stub when SDK is missing so targets.* is available")
    void execute_missingSdk_providesTargetsStub() {
        // arrange
        ReplayExecutor sut = new ReplayExecutor(om, engine);
        String js = "function transform(){ return typeof targets.synchronizeProducts.defineUpsert === 'function'; }";
        JsonNode source = makeSource(om, Map.of());

        // act
        ReplayExecutor.Result res = sut.execute("test-stub.js", js, source, null);

        // assert
        assertNull(res.errorInfo());
        assertEquals(true, res.outputData());
    }

    @Test
    @DisplayName("execute(): returns JSON serialization error when ObjectMapper fails")
    void execute_jsonSerializationFailure_returnsErrorInfo() {
        // arrange: ObjectMapper that always throws on writeValueAsString
        ObjectMapper throwingOm = new ObjectMapper() {
            private static final long serialVersionUID = 1L;

            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("boom") {
                    private static final long serialVersionUID = 1L;
                };
            }
        };
        ReplayExecutor sut = new ReplayExecutor(throwingOm, engine);
        JsonNode source = makeSource(om, Map.of("x", 1));
        String js = "function transform(){ return 1; }";

        // act
        ReplayExecutor.Result res = sut.execute("test-json.js", js, source, "");

        // assert
        assertNull(res.outputData());
        assertNotNull(res.errorInfo());
        assertTrue(res.errorInfo().startsWith("JSON serialization error"));
    }
}
