package de.unistuttgart.stayinsync.core.scriptengine;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(ScriptApiTests.ScriptApiTestProfile.class)
public class ScriptApiTests {

    private static final String TEST_JOB_ID = "test-job-123";
    private ScriptApi scriptApi;

    private static CapturingLogHandler capturingLogHandler;
    private static Logger scriptExecutionLoggerJul;

    public static class ScriptApiTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "org.jboss.logging.provider", "jdk",
                    "quarkus.log.category.\"ScriptExecutionLogger\".level", "ALL",
                    "quarkus.log.category.\"ScriptExecutionLogger\".min-level", "ALL",
                    "quarkus.log.console.enable", "false"
            );
        }
    }

    public static class CapturingLogHandler extends Handler {
        private final List<LogRecord> records = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }

        public List<LogRecord> getRecords() {
            return new ArrayList<>(records);
        }

        public void clearRecords() {
            records.clear();
        }
    }

    @BeforeEach
    void setUp() {
        MDC.clear();

        capturingLogHandler = new CapturingLogHandler();
        capturingLogHandler.setLevel(Level.ALL);

        scriptExecutionLoggerJul = Logger.getLogger(ScriptApi.class.getName());

        for (Handler existingHandler : scriptExecutionLoggerJul.getHandlers()) {
            scriptExecutionLoggerJul.removeHandler(existingHandler);
        }

        scriptExecutionLoggerJul.addHandler(capturingLogHandler);
        scriptExecutionLoggerJul.setLevel(Level.ALL);
        scriptExecutionLoggerJul.setUseParentHandlers(false);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        if (scriptExecutionLoggerJul != null && capturingLogHandler != null) {
            scriptExecutionLoggerJul.removeHandler(capturingLogHandler);
        }
    }

    private List<LogRecord> getCapturedLogRecords() {
        return capturingLogHandler.getRecords();
    }

    @Test
    void constructor_shouldStoreJobId() {
        Object input = "testInput";
        scriptApi = new ScriptApi(input, TEST_JOB_ID);
        assertNotNull(scriptApi);
    }

    @Test
    void getInput_withSimpleObject_shouldReturnSameObject() {
        String input = "simpleString";
        scriptApi = new ScriptApi(input, TEST_JOB_ID);
        assertEquals(input, scriptApi.getInput());

        Integer numberInput = 123;
        scriptApi = new ScriptApi(numberInput, TEST_JOB_ID);
        assertEquals(numberInput, scriptApi.getInput());
    }

    @Test
    void getInput_withNull_shouldReturnNull() {
        scriptApi = new ScriptApi(null, TEST_JOB_ID);
        assertNull(scriptApi.getInput());
    }

    @Test
    void getInput_withMap_shouldReturnUnmodifiableMapAndNotAffectOriginal() {
        Map<String, Object> originalMap = new HashMap<>();
        originalMap.put("key1", "value1");
        originalMap.put("key2", 123);

        scriptApi = new ScriptApi(new HashMap<>(originalMap), TEST_JOB_ID);
        Object result = scriptApi.getInput();

        assertInstanceOf(Map.class, result, "Result should be a Map");
        Map<?, ?> resultMap = (Map<?, ?>) result;

        assertEquals("value1", resultMap.get("key1"));
        assertEquals(123, resultMap.get("key2"));

        assertThrows(UnsupportedOperationException.class, () -> {
            Map<Object, Object> rawMap = (Map<Object, Object>) resultMap;
            rawMap.put("key3", "newValue");
        }, "Returned map should be unmodifiable");

        originalMap.put("key1", "modifiedValueInOriginal");
        assertEquals("value1", resultMap.get("key1"),
                "Returned map should not reflect later changes to the original external map.");
    }

    @Test
    void getInput_withNestedMap_shouldReturnUnmodifiableNestedMapsAndNotAffectOriginals() {
        Map<String, Object> originalNestedMap = new HashMap<>();
        originalNestedMap.put("nestedKey", "nestedValue");

        Map<String, Object> originalOuterMap = new HashMap<>();
        originalOuterMap.put("key1", "value1");
        originalOuterMap.put("nestedMap", new HashMap<>(originalNestedMap));

        scriptApi = new ScriptApi(new HashMap<>(originalOuterMap), TEST_JOB_ID);
        Object result = scriptApi.getInput();

        assertInstanceOf(Map.class, result, "Result should be a Map");
        Map<?, ?> resultMap = (Map<?, ?>) result;

        assertEquals("value1", resultMap.get("key1"));
        assertThrows(UnsupportedOperationException.class, () -> {
            Map<Object, Object> rawOuterMap = (Map<Object, Object>) resultMap;
            rawOuterMap.put("key3", "newValue");
        }, "Outer map unmodifiable");

        Object nestedResultObj = resultMap.get("nestedMap");
        assertInstanceOf(Map.class, nestedResultObj, "Nested object should be a Map");
        Map<?, ?> nestedResultMap = (Map<?, ?>) nestedResultObj;

        assertEquals("nestedValue", nestedResultMap.get("nestedKey"));
        assertThrows(UnsupportedOperationException.class, () -> {
            Map<Object, Object> rawNestedMap = (Map<Object, Object>) nestedResultMap;
            rawNestedMap.put("newNestedKey", "newNestedValue");
        }, "Nested map should be unmodifiable");

        originalNestedMap.put("nestedKey", "modifiedOriginalNested");
        assertEquals("nestedValue", nestedResultMap.get("nestedKey"), "Returned nested map immune to original nested changes");

        originalOuterMap.put("key1", "modifiedOriginalOuter");
        assertEquals("value1", resultMap.get("key1"), "Returned outer map immune to original outer changes");
    }

    @Test
    void getInput_withMapContainingMutableNonMapObject_shouldReturnReference() {
        List<String> listValue = new ArrayList<>();
        listValue.add("item1");

        Map<String, Object> originalMap = new HashMap<>();
        originalMap.put("listKey", listValue);

        scriptApi = new ScriptApi(originalMap, TEST_JOB_ID);
        Map<?, ?> resultMap = (Map<?, ?>) scriptApi.getInput();

        Object listFromResultMap = resultMap.get("listKey");
        assertSame(listValue, listFromResultMap,
                "Non-map mutable objects inside the map should be the same instance (shallow copy for non-map values)");

        listValue.add("item2");
        assertTrue(((List<?>) listFromResultMap).contains("item2"), "Change to original list should be visible in returned map's list");
    }

    @Test
    void log_shouldSetAndRemoveMdcJobId() {
        scriptApi = new ScriptApi("input", TEST_JOB_ID);
        assertNull(MDC.get("jobId"), "MDC jobId should be null before log call");
        scriptApi.log("test message", "INFO");
        assertNull(MDC.get("jobId"), "MDC jobId should be null after log call's finally block");
    }

    @ParameterizedTest
    @CsvSource({
            "INFO, INFO",
            "WARN, WARNING",
            "ERROR, SEVERE",
            "DEBUG, FINE",
            "TRACE, FINER",
            "info, INFO",
            "WaRn, WARNING"
    })
    void log_withValidLogLevels_shouldUseCorrectLevelAndNotWarn(String inputLevelStr, String expectedJulLevelStr) {
        scriptApi = new ScriptApi("input", TEST_JOB_ID);
        scriptApi.log("Test message for " + inputLevelStr, inputLevelStr);

        List<LogRecord> records = getCapturedLogRecords();
        assertEquals(1, records.size(), "Should only be one log record for valid level. Records: " + records);

        LogRecord record = records.getFirst();
        assertEquals("Test message for " + inputLevelStr, record.getMessage());
        assertEquals(Level.parse(expectedJulLevelStr), record.getLevel(), "Log level mismatch for input: " + inputLevelStr);

        long warnMessagesCount = records.stream()
                .filter(r -> r.getLevel() == Level.WARNING &&
                        r.getMessage().startsWith("Invalid log level"))
                .count();
        assertEquals(0, warnMessagesCount, "No 'Invalid log level' warning should be logged for valid input: " + inputLevelStr);
    }

    @Test
    void log_whenLogLevelIsNull_shouldLogAsInfoAndNotWarn() {
        scriptApi = new ScriptApi("inputData", TEST_JOB_ID);
        scriptApi.log("Message with null level", null);

        List<LogRecord> records = getCapturedLogRecords();
        assertEquals(1, records.size(), "Should only be one log record for null level.");

        LogRecord mainRecord = records.getFirst();
        assertEquals("Message with null level", mainRecord.getMessage());
        assertEquals(Level.INFO, mainRecord.getLevel(), "Log level should default to INFO for null input.");

        long warnMessagesCount = records.stream()
                .filter(r -> r.getLevel() == Level.WARNING &&
                        r.getMessage().startsWith("Invalid log level"))
                .count();
        assertEquals(0, warnMessagesCount, "No 'Invalid log level' warning should be logged for null log level.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID_LEVEL", "information"})
    void log_withTrulyInvalidLogLevel_shouldDefaultToInfoAndWarn(String logLevel) {
        scriptApi = new ScriptApi("input", TEST_JOB_ID);
        scriptApi.log("Test message with problematic level", logLevel);

        List<LogRecord> records = getCapturedLogRecords();

        assertEquals(2, records.size(), "Expected one main log (INFO) and one warning log for: '" + logLevel + "'. Records in test list: " + records.stream().map(r -> r.getLoggerName() + ": " + r.getMessage()).toList());

        LogRecord mainRecord = records.stream()
                .filter(r -> r.getMessage().equals("Test message with problematic level"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Main log message not found for level: " + logLevel));
        assertEquals(Level.INFO, mainRecord.getLevel(), "Main message should default to INFO level for: " + logLevel);

        LogRecord warnRecord = records.stream()
                .filter(r -> {
                    boolean levelMatch = r.getLevel().equals(Level.WARNING);
                    boolean messageMatch = r.getMessage().startsWith("Invalid log level '" + logLevel + "'");
                    return levelMatch && messageMatch;
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("Warning log message not found for level: " + logLevel + ". Available records in test list: " +
                        records.stream().map(r -> "L:" + r.getLevel() + " M:" + r.getMessage()).toList()));
        assertTrue(warnRecord.getMessage().contains("Defaulting to INFO."), "Warning message content is incorrect for: " + logLevel);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void log_withEmptyOrBlankLogLevel_shouldDefaultToInfoAndNotWarn(String logLevel) {
        scriptApi = new ScriptApi("input", TEST_JOB_ID);
        scriptApi.log("Test message with empty/blank level", logLevel);

        List<LogRecord> records = getCapturedLogRecords();
        assertEquals(1, records.size(), "Should only be one log record for empty/blank level.");

        LogRecord mainRecord = records.getFirst();
        assertEquals("Test message with empty/blank level", mainRecord.getMessage());
        assertEquals(Level.INFO, mainRecord.getLevel(), "Log level should default to INFO for empty/blank input.");

        long warnMessagesCount = records.stream()
                .filter(r -> r.getLevel() == Level.WARNING &&
                        r.getMessage().startsWith("Invalid log level"))
                .count();
        assertEquals(0, warnMessagesCount, "No 'Invalid log level' warning should be logged for empty/blank log level.");
    }
}