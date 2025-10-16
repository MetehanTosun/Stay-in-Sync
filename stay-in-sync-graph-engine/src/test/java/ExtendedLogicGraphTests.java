import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.exception.NodeConfigurationException;
import de.unistuttgart.graphengine.logic_engine.LogicGraphEvaluator;
import de.unistuttgart.graphengine.logic_operator.LogicOperator;
import de.unistuttgart.graphengine.nodes.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended tests for LogicGraphEvaluator with various predicates and operators,
 * including the new change detection logic.
 */
public class ExtendedLogicGraphTests {

    private LogicGraphEvaluator evaluator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        evaluator = new LogicGraphEvaluator();
    }

    // =====================================================================================
    // CORE EVALUATOR TESTS - Basic functionality and error handling
    // =====================================================================================

    @Test
    public void testEvaluateGraph_NullGraphThrowsException() {
        GraphEvaluationException exception = assertThrows(
                GraphEvaluationException.class,
                () -> evaluator.evaluateGraph(null, new HashMap<>())
        );

        assertEquals(GraphEvaluationException.ErrorType.INVALID_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }

    @Test
    public void testEvaluateGraph_EmptyGraphThrowsException() {
        GraphEvaluationException exception = assertThrows(
                GraphEvaluationException.class,
                () -> evaluator.evaluateGraph(List.of(), new HashMap<>())
        );

        assertEquals(GraphEvaluationException.ErrorType.INVALID_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("cannot be null or empty"));
    }

    @Test
    public void testEvaluateGraph_NodeCalculationFailureThrowsException() {
        assertDoesNotThrow(() -> {
            ProviderNode invalidProvider = createProviderNode("source.sensor.nonexistent.deeply.nested.path", 0);
            FinalNode finalNode = createFinalNode(1, invalidProvider);
            List<Node> graph = Arrays.asList(invalidProvider, finalNode);

            Map<String, Object> context = createDataContext("{}", null);

            GraphEvaluationException exception = assertThrows(
                    GraphEvaluationException.class,
                    () -> evaluator.evaluateGraph(graph, context)
            );

            assertEquals(GraphEvaluationException.ErrorType.EXECUTION_FAILED, exception.getErrorType());
            assertTrue(exception.getMessage().contains("unexpected error occurred"));
        });
    }

    @Test
    public void testEvaluateGraph_NullDataContextHandling() {
        assertDoesNotThrow(() -> {
            ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
            FinalNode finalNode = createFinalNode(1, tempProvider);
            List<Node> graph = Arrays.asList(tempProvider, finalNode);

            assertThrows(
                    Exception.class,
                    () -> evaluator.evaluateGraph(graph, null)
            );
        });
    }

    @Test
    public void testEvaluateGraph_ResultResetBetweenEvaluations() {
        assertDoesNotThrow(() -> {
            ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
            ConstantNode threshold = createConstantNode("Threshold", 20.0, 1);
            LogicNode comparison = createLogicNode("TempCheck", LogicOperator.GREATER_THAN, 2, tempProvider, threshold);
            FinalNode finalNode = createFinalNode(3, comparison);
            List<Node> graph = Arrays.asList(tempProvider, threshold, comparison, finalNode);

            // First evaluation - should be true
            Map<String, Object> context1 = createDataContext("{\"temperature\": 25.0}", null);
            boolean result1 = evaluator.evaluateGraph(graph, context1);
            assertTrue(result1);

            // Second evaluation with different data - should be false
            Map<String, Object> context2 = createDataContext("{\"temperature\": 15.0}", null);
            boolean result2 = evaluator.evaluateGraph(graph, context2);
            assertFalse(result2);

            assertNotEquals(result1, result2);
        });
    }

    // =====================================================================================
    // CHANGE DETECTION TESTS - ConfigNode functionality
    // =====================================================================================

    @Test
    public void testChangeDetection_OrMode() {
        assertDoesNotThrow(() -> {
            // Test: Trigger if temperature OR humidity changes (OR-Mode)
            ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
            ProviderNode humidityProvider = createProviderNode("source.sensor.humidity", 1);

            ConfigNode configNode = createConfigNode(2, tempProvider, humidityProvider);
            configNode.setMode(ConfigNode.ChangeDetectionMode.OR);

            FinalNode finalNode = createFinalNode(3, configNode);
            List<Node> graph = Arrays.asList(tempProvider, humidityProvider, configNode, finalNode);

            // Test Case 1: First run (no snapshot), should initialize snapshot -> false
            Map<String, Object> context1 = createDataContext("{\"temperature\": 20.0, \"humidity\": 50}", null);
            boolean result1 = evaluator.evaluateGraph(graph, context1);
            assertFalse(result1, "Should return false on first run (only initialize snapshot)");
            assertNotNull(configNode.getNewSnapshotData(), "Should create a new snapshot on first run");

            // Test Case 2: Second run, no change -> false
            Map<String, SnapshotEntry> snapshotForRun2 = configNode.getNewSnapshotData();
            Map<String, Object> context2 = createDataContext("{\"temperature\": 20.0, \"humidity\": 50}", snapshotForRun2);
            boolean result2 = evaluator.evaluateGraph(graph, context2);
            assertFalse(result2, "Should not detect change when data is identical to snapshot");
        });
    }

    @Test
    public void testChangeDetection_AndMode() {
        assertDoesNotThrow(() -> {
            // Test: Trigger ONLY if temperature AND humidity change (AND-Mode)
            ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
            ProviderNode humidityProvider = createProviderNode("source.sensor.humidity", 1);
            ConfigNode configNode = createConfigNode(2, tempProvider, humidityProvider);
            configNode.setMode(ConfigNode.ChangeDetectionMode.AND);
            FinalNode finalNode = createFinalNode(3, configNode);
            List<Node> graph = Arrays.asList(tempProvider, humidityProvider, configNode, finalNode);

            Map<String, SnapshotEntry> snapshot = Map.of(
                    "source.sensor.temperature", new SnapshotEntry(20.0, 0L),
                    "source.sensor.humidity", new SnapshotEntry(50, 0L)
            );

            // Case 1: Only one variable changes -> false
            Map<String, Object> context1 = createDataContext("{\"temperature\": 21.0, \"humidity\": 50}", snapshot);
            assertFalse(evaluator.evaluateGraph(graph, context1), "Should be false if only one value changes in AND mode");

            // Case 2: No variable changes -> false
            Map<String, Object> context2 = createDataContext("{\"temperature\": 20.0, \"humidity\": 50}", snapshot);
            assertFalse(evaluator.evaluateGraph(graph, context2), "Should be false if no value changes in AND mode");

            // Case 3: Both variables change -> true
            Map<String, Object> context3 = createDataContext("{\"temperature\": 21.0, \"humidity\": 51}", snapshot);
            assertTrue(evaluator.evaluateGraph(graph, context3), "Should be true if both values change in AND mode");
        });
    }

    @Test
    public void testChangeDetection_BypassSwitch() {
        assertDoesNotThrow(() -> {
            // Test: The bypass switch on the ConfigNode disables the check
            ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
            ConfigNode configNode = createConfigNode(1, tempProvider);
            configNode.setActive(false); // Bypass is ACTIVE (change detection is OFF)
            FinalNode finalNode = createFinalNode(2, configNode);
            List<Node> graph = Arrays.asList(tempProvider, configNode, finalNode);

            Map<String, SnapshotEntry> snapshot = Map.of("source.sensor.temperature", new SnapshotEntry(20.0, 0L));
            Map<String, Object> context = createDataContext("{\"temperature\": 25.0}", snapshot);

            assertFalse(evaluator.evaluateGraph(graph, context),
                    "Should be false when bypass is active, even if data changed");
        });
    }

    @Test
    public void testChangeDetection_WithTimeWindow() {
        assertDoesNotThrow(() -> {
            ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
            ProviderNode humidityProvider = createProviderNode("source.sensor.humidity", 1);

            ConfigNode configNode = createConfigNode(2, tempProvider, humidityProvider);
            configNode.setMode(ConfigNode.ChangeDetectionMode.AND);
            configNode.setTimeWindowEnabled(true);
            configNode.setTimeWindowMillis(10000);

            FinalNode finalNode = createFinalNode(3, configNode);
            List<Node> graph = Arrays.asList(tempProvider, humidityProvider, configNode, finalNode);

            long timeRun1 = System.currentTimeMillis();
            long timeRun2 = timeRun1 + 5000;
            long timeRun3 = timeRun1 + 12000;

            Map<String, SnapshotEntry> initialSnapshot = Map.of(
                    "source.sensor.temperature", new SnapshotEntry(20.0, timeRun1 - 20000),
                    "source.sensor.humidity", new SnapshotEntry(50, timeRun1 - 20000)
            );
            Map<String, Object> context1 = createDataContext("{\"temperature\": 21.0, \"humidity\": 50}", initialSnapshot);
            injectCurrentTime(configNode, timeRun1);
            boolean result1 = evaluator.evaluateGraph(graph, context1);
            assertFalse(result1, "Should be false since only one value changed.");

            Map<String, SnapshotEntry> snapshotForRun2 = configNode.getNewSnapshotData();
            Map<String, Object> context2 = createDataContext("{\"temperature\": 21.0, \"humidity\": 55}", snapshotForRun2);
            injectCurrentTime(configNode, timeRun2);
            boolean result2 = evaluator.evaluateGraph(graph, context2);
            assertTrue(result2, "Should be true since both changes occurred within the window.");

            Map<String, SnapshotEntry> snapshotForRun3 = snapshotForRun2;
            Map<String, Object> context3 = createDataContext("{\"temperature\": 21.0, \"humidity\": 55}", snapshotForRun3);
            injectCurrentTime(configNode, timeRun3);
            boolean result3 = evaluator.evaluateGraph(graph, context3);
            assertFalse(result3, "Should be false since the first change is now outside the window.");
        });
    }

    @Test
    public void testChangeDetection_CombinedWithOperator() {
        assertDoesNotThrow(() -> {
            ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
            ConfigNode configNode = createConfigNode(1, tempProvider);
            configNode.setActive(true);

            ProviderNode statusProvider = createProviderNode("source.sensor.status", 2);
            ConstantNode errorStatus = createConstantNode("ErrorStatus", "ERROR", 3);
            LogicNode statusCheck = createLogicNode("StatusCheckIsError", LogicOperator.EQUALS, 4, statusProvider, errorStatus);

            LogicNode combinedOr = createLogicNode("CombinedOr", LogicOperator.OR, 5, configNode, statusCheck);
            FinalNode finalNode = createFinalNode(6, combinedOr);

            List<Node> graph = Arrays.asList(tempProvider, configNode, statusProvider, errorStatus, statusCheck, combinedOr, finalNode);

            Map<String, SnapshotEntry> snapshot1 = Map.of("source.sensor.temperature", new SnapshotEntry(20.0, 0L));
            Map<String, Object> context1 = createDataContext("{\"temperature\": 20.0, \"status\": \"OK\"}", snapshot1);
            assertFalse(evaluator.evaluateGraph(graph, context1), "Should be false when no change and status is OK");

            Map<String, SnapshotEntry> snapshot2 = configNode.getNewSnapshotData();
            Map<String, Object> context2 = createDataContext("{\"temperature\": 21.0, \"status\": \"OK\"}", snapshot2);
            assertTrue(evaluator.evaluateGraph(graph, context2), "Should be true when temperature changes");

            Map<String, SnapshotEntry> snapshot3 = configNode.getNewSnapshotData();
            Map<String, Object> context3 = createDataContext("{\"temperature\": 21.0, \"status\": \"ERROR\"}", snapshot3);
            assertTrue(evaluator.evaluateGraph(graph, context3), "Should be true when status is ERROR");
        });
    }

    // =====================================================================================
    // NUMBER PREDICATE TESTS
    // =====================================================================================

    @Test
    public void testNumberPredicates_LessThan() {
        assertDoesNotThrow(() -> {
            ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
            ConstantNode threshold = createConstantNode("Threshold", 20.0, 1);
            LogicNode comparison = createLogicNode("TempCheck", LogicOperator.LESS_THAN, 2, tempProvider, threshold);
            FinalNode finalNode = createFinalNode(3, comparison);
            List<Node> graph = Arrays.asList(tempProvider, threshold, comparison, finalNode);

            Map<String, Object> context1 = createDataContext("{\"temperature\": 15.5}", null);
            assertTrue(evaluator.evaluateGraph(graph, context1));

            Map<String, Object> context2 = createDataContext("{\"temperature\": 25.0}", null);
            assertFalse(evaluator.evaluateGraph(graph, context2));
        });
    }

    @Test
    public void testNumberPredicates_Between() {
        assertDoesNotThrow(() -> {
            ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
            ConstantNode lowerBound = createConstantNode("LowerBound", 10.0, 1);
            ConstantNode upperBound = createConstantNode("UpperBound", 30.0, 2);
            LogicNode betweenCheck = createLogicNode("BetweenCheck", LogicOperator.BETWEEN, 3, tempProvider, lowerBound, upperBound);
            FinalNode finalNode = createFinalNode(4, betweenCheck);
            List<Node> graph = Arrays.asList(tempProvider, lowerBound, upperBound, betweenCheck, finalNode);

            assertTrue(evaluator.evaluateGraph(graph, createDataContext("{\"temperature\": 20.0}", null)));
            assertFalse(evaluator.evaluateGraph(graph, createDataContext("{\"temperature\": 5.0}", null)));
            assertFalse(evaluator.evaluateGraph(graph, createDataContext("{\"temperature\": 35.0}", null)));
        });
    }

    // =====================================================================================
    // BOOLEAN PREDICATE TESTS
    // =====================================================================================

    @Test
    public void testBooleanPredicates() {
        assertDoesNotThrow(() -> {
            ProviderNode activeProvider = createProviderNode("source.sensor.isActive", 0);
            ProviderNode maintenanceProvider = createProviderNode("source.sensor.maintenanceMode", 1);
            LogicNode activeCheck = createLogicNode("ActiveCheck", LogicOperator.IS_TRUE, 2, activeProvider);
            LogicNode maintenanceCheck = createLogicNode("MaintenanceCheck", LogicOperator.IS_FALSE, 3, maintenanceProvider);
            LogicNode finalAnd = createLogicNode("FinalCheck", LogicOperator.AND, 4, activeCheck, maintenanceCheck);
            FinalNode finalNode = createFinalNode(5, finalAnd);
            List<Node> graph = Arrays.asList(activeProvider, maintenanceProvider, activeCheck, maintenanceCheck, finalAnd, finalNode);

            assertTrue(evaluator.evaluateGraph(graph, createDataContext("{\"isActive\": true, \"maintenanceMode\": false}", null)));
            assertFalse(evaluator.evaluateGraph(graph, createDataContext("{\"isActive\": false, \"maintenanceMode\": false}", null)));
            assertFalse(evaluator.evaluateGraph(graph, createDataContext("{\"isActive\": true, \"maintenanceMode\": true}", null)));
        });
    }

    // =====================================================================================
    // STRING PREDICATE TESTS
    // =====================================================================================

    @Test
    public void testStringPredicates() {
        assertDoesNotThrow(() -> {
            ProviderNode deviceNameProvider = createProviderNode("source.sensor.deviceName", 0);
            ProviderNode statusProvider = createProviderNode("source.sensor.status", 1);
            ConstantNode sensorPattern = createConstantNode("SensorPattern", "Sensor", 2);
            ConstantNode okStatus = createConstantNode("OKStatus", "OK", 3);
            LogicNode nameCheck = createLogicNode("NameCheck", LogicOperator.STRING_CONTAINS, 4, deviceNameProvider, sensorPattern);
            LogicNode statusCheck = createLogicNode("StatusCheck", LogicOperator.EQUALS, 5, statusProvider, okStatus);
            LogicNode finalAnd = createLogicNode("FinalCheck", LogicOperator.AND, 6, nameCheck, statusCheck);
            FinalNode finalNode = createFinalNode(7, finalAnd);
            List<Node> graph = Arrays.asList(deviceNameProvider, statusProvider, sensorPattern, okStatus, nameCheck, statusCheck, finalAnd, finalNode);

            assertTrue(evaluator.evaluateGraph(graph, createDataContext("{\"deviceName\": \"TemperatureSensor\", \"status\": \"OK\"}", null)));
            assertFalse(evaluator.evaluateGraph(graph, createDataContext("{\"deviceName\": \"Thermometer\", \"status\": \"OK\"}", null)));
        });
    }

    // =====================================================================================
    // ARRAY PREDICATE TESTS
    // =====================================================================================

    @Test
    public void testArrayPredicates() {
        assertDoesNotThrow(() -> {
            ProviderNode sensorsProvider = createProviderNode("source.sensor.activeSensors", 0);
            ConstantNode temperatureSensorName = createConstantNode("TempSensorName", "temperature", 1);
            ConstantNode minSensorCount = createConstantNode("MinSensorCount", 2, 2);
            LogicNode containsCheck = createLogicNode("ContainsTemperatureSensor", LogicOperator.CONTAINS_ELEMENT, 3, sensorsProvider, temperatureSensorName);
            LogicNode lengthCheck = createLogicNode("SufficientSensors", LogicOperator.LENGTH_GT, 4, sensorsProvider, minSensorCount);
            LogicNode finalAnd = createLogicNode("FinalCheck", LogicOperator.AND, 5, containsCheck, lengthCheck);
            FinalNode finalNode = createFinalNode(6, finalAnd);
            List<Node> graph = Arrays.asList(sensorsProvider, temperatureSensorName, minSensorCount, containsCheck, lengthCheck, finalAnd, finalNode);

            assertTrue(evaluator.evaluateGraph(graph, createDataContext("{\"activeSensors\": [\"temperature\", \"humidity\", \"pressure\"]}", null)));
            assertFalse(evaluator.evaluateGraph(graph, createDataContext("{\"activeSensors\": [\"humidity\", \"pressure\"]}", null)));
            assertFalse(evaluator.evaluateGraph(graph, createDataContext("{\"activeSensors\": [\"temperature\"]}", null)));
        });
    }

    // =====================================================================================
    // OBJECT PREDICATE TESTS
    // =====================================================================================

    @Test
    public void testObjectPredicates() {
        assertDoesNotThrow(() -> {
            ProviderNode configProvider = createProviderNode("source.sensor.config", 0);
            ConstantNode requiredKeys = createConstantNode("RequiredKeys", Arrays.asList("threshold", "interval"), 1);
            ConstantNode thresholdKey = createConstantNode("ThresholdKey", "threshold", 2);
            LogicNode hasAllKeysCheck = createLogicNode("HasAllRequiredKeys", LogicOperator.HAS_ALL_KEYS, 3, configProvider, requiredKeys);
            LogicNode hasThresholdCheck = createLogicNode("HasThreshold", LogicOperator.HAS_KEY, 4, configProvider, thresholdKey);
            LogicNode finalAnd = createLogicNode("FinalCheck", LogicOperator.AND, 5, hasAllKeysCheck, hasThresholdCheck);
            FinalNode finalNode = createFinalNode(6, finalAnd);
            List<Node> graph = Arrays.asList(configProvider, requiredKeys, thresholdKey, hasAllKeysCheck, hasThresholdCheck, finalAnd, finalNode);

            assertTrue(evaluator.evaluateGraph(graph, createDataContext("{\"config\": {\"threshold\": 25.0, \"interval\": 5000, \"name\": \"TempSensor\"}}", null)));
            assertFalse(evaluator.evaluateGraph(graph, createDataContext("{\"config\": {\"threshold\": 25.0, \"name\": \"TempSensor\"}}", null)));
        });
    }

    // =====================================================================================
    // EXISTENCE PREDICATE TESTS
    // =====================================================================================

    @Test
    public void testExistencePredicates() {
        assertDoesNotThrow(() -> {
            ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
            ProviderNode pressureProvider = createProviderNode("source.sensor.pressure", 1);
            ProviderNode nonExistentProvider = createProviderNode("source.sensor.nonexistent", 2);
            LogicNode tempExistsCheck = createLogicNode("TempExists", LogicOperator.EXISTS, 3, tempProvider);
            LogicNode pressureExistsCheck = createLogicNode("PressureExists", LogicOperator.EXISTS, 4, pressureProvider);
            LogicNode nonExistentCheck = createLogicNode("NonExistentCheck", LogicOperator.NOT_EXISTS, 5, nonExistentProvider);
            LogicNode tempAndPressureExist = createLogicNode("TempAndPressureExist", LogicOperator.AND, 6, tempExistsCheck, pressureExistsCheck);
            LogicNode finalAnd = createLogicNode("FinalCheck", LogicOperator.AND, 7, tempAndPressureExist, nonExistentCheck);
            FinalNode finalNode = createFinalNode(8, finalAnd);
            List<Node> graph = Arrays.asList(tempProvider, pressureProvider, nonExistentProvider, tempExistsCheck, pressureExistsCheck, nonExistentCheck, tempAndPressureExist, finalAnd, finalNode);

            assertTrue(evaluator.evaluateGraph(graph, createDataContext("{\"temperature\": 25.0, \"pressure\": 1013.25}", null)));
            assertFalse(evaluator.evaluateGraph(graph, createDataContext("{\"temperature\": 25.0}", null)));
        });
    }

    // =====================================================================================
    // COMPLEX LOGICAL OPERATOR TESTS
    // =====================================================================================

    @Test
    public void testComplexLogicalOperators() {
        assertDoesNotThrow(() -> {
            ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
            ProviderNode humidityProvider = createProviderNode("source.sensor.humidity", 1);
            ProviderNode maintenanceProvider = createProviderNode("source.sensor.maintenanceMode", 2);
            ConstantNode tempThreshold = createConstantNode("TempThreshold", 25.0, 3);
            ConstantNode humidityThreshold = createConstantNode("HumidityThreshold", 80.0, 4);
            LogicNode tempCheck = createLogicNode("TempCheck", LogicOperator.GREATER_THAN, 5, tempProvider, tempThreshold);
            LogicNode humidityCheck = createLogicNode("HumidityCheck", LogicOperator.GREATER_THAN, 6, humidityProvider, humidityThreshold);
            LogicNode tempOrHumidity = createLogicNode("TempOrHumidity", LogicOperator.OR, 7, tempCheck, humidityCheck);
            LogicNode notMaintenance = createLogicNode("NotMaintenance", LogicOperator.NOT, 8, maintenanceProvider);
            LogicNode finalAnd = createLogicNode("FinalCheck", LogicOperator.AND, 9, tempOrHumidity, notMaintenance);
            FinalNode finalNode = createFinalNode(10, finalAnd);
            List<Node> graph = Arrays.asList(tempProvider, humidityProvider, maintenanceProvider, tempThreshold, humidityThreshold, 
                    tempCheck, humidityCheck, tempOrHumidity, notMaintenance, finalAnd, finalNode);

            assertTrue(evaluator.evaluateGraph(graph, createDataContext("{\"temperature\": 30.0, \"humidity\": 60.0, \"maintenanceMode\": false}", null)));
        });
    }

    // =====================================================================================
    // HELPER METHODS
    // =====================================================================================

    private Map<String, Object> createDataContext(String json, Map<String, SnapshotEntry> snapshot) throws IOException {
        JsonNode data = objectMapper.readTree(json);
        ObjectNode sourceNode = objectMapper.createObjectNode();
        sourceNode.set("sensor", data);

        Map<String, Object> dataContext = new HashMap<>();
        dataContext.put("source", sourceNode);

        if (snapshot != null) {
            dataContext.put("__snapshot", snapshot);
        }
        return dataContext;
    }

    private void injectCurrentTime(ConfigNode node, long timeMillis) {
        node.setTestTime(timeMillis);
    }

    private ProviderNode createProviderNode(String jsonPath, int id) throws NodeConfigurationException {
        ProviderNode node = new ProviderNode(jsonPath);
        node.setId(id);
        return node;
    }

    private ConstantNode createConstantNode(String name, Object value, int id) throws NodeConfigurationException {
        ConstantNode node = new ConstantNode(name, value);
        node.setId(id);
        return node;
    }

    private LogicNode createLogicNode(String name, LogicOperator operator, int id, Node... inputs) throws NodeConfigurationException {
        LogicNode node = new LogicNode(name, operator);
        node.setInputNodes(Arrays.asList(inputs));
        node.setId(id);
        return node;
    }

    private FinalNode createFinalNode(int id, Node input) {
        FinalNode node = new FinalNode();
        node.setId(id);
        node.setInputNodes(List.of(input));
        return node;
    }

    private ConfigNode createConfigNode(int id, Node... inputs) {
        ConfigNode node = new ConfigNode();
        node.setId(id);
        node.setInputNodes(Arrays.asList(inputs));
        return node;
    }
}
