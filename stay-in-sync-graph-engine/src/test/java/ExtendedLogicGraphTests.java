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

    @Test
    public void testChangeDetection_WithTimeWindow() throws IOException, GraphEvaluationException {
        System.out.println("--- TEST: Time Window Feature ('Sliding Window') ---");

        // ARRANGE
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ProviderNode humidityProvider = createProviderNode("source.sensor.humidity", 1);

        // ACCEPTANCE CRITERIA 1: User can activate time window and set duration
        ConfigNode configNode = createConfigNode(2, tempProvider, humidityProvider);
        configNode.setMode(ConfigNode.ChangeDetectionMode.AND);
        configNode.setTimeWindowEnabled(true);
        configNode.setTimeWindowMillis(10000); // 10 seconds
        System.out.println("[SETUP] Acceptance Criteria 1: ConfigNode created. Mode=AND, Time Window=10s activated.");

        FinalNode finalNode = createFinalNode(3, configNode);
        List<Node> graph = Arrays.asList(tempProvider, humidityProvider, configNode, finalNode);

        // --- Time Simulation ---
        long timeRun1 = System.currentTimeMillis();
        long timeRun2 = timeRun1 + 5000;  // 5 seconds after Run 1
        long timeRun3 = timeRun1 + 12000; // 12 seconds after Run 1

        // === RUN 1: One of two changes occurs ===
        System.out.println("\n[RUN 1] Situation: Only temperature changes.");
        Map<String, SnapshotEntry> initialSnapshot = Map.of(
                "source.sensor.temperature", new SnapshotEntry(20.0, timeRun1 - 20000),
                "source.sensor.humidity", new SnapshotEntry(50, timeRun1 - 20000)
        );
        Map<String, Object> context1 = createDataContext("{\"temperature\": 21.0, \"humidity\": 50}", initialSnapshot);

        injectCurrentTime(configNode, timeRun1);
        boolean result1 = evaluator.evaluateGraph(graph, context1);

        System.out.println("[RUN 1] RESULT: Transformation was NOT triggered (false).");
        assertFalse(result1, "Should be false since only one value changed.");

        // Get new snapshot from ConfigNode after first run
        Map<String, SnapshotEntry> snapshotForRun2 = configNode.getNewSnapshotData();

        // === RUN 2: Second change WITHIN the window ===
        System.out.println("\n[RUN 2] Situation: Second change occurs 5s later (within the 10s window).");
        Map<String, Object> context2 = createDataContext("{\"temperature\": 21.0, \"humidity\": 55}", snapshotForRun2);

        injectCurrentTime(configNode, timeRun2);
        boolean result2 = evaluator.evaluateGraph(graph, context2);

        System.out.println("[RUN 2] RESULT: Transformation WAS triggered (true).");
        // ACCEPTANCE CRITERIA 2: Transformation is triggered when all changes are within the window
        assertTrue(result2, "Should be true since both changes occurred within the window.");
        System.out.println("✓ Acceptance Criteria 1 met: Transformation triggered for changes within time window.");

        // === RUN 3: Second change OUTSIDE the window ===
        System.out.println("\n[RUN 3] Situation: Second change occurs 12s after the first (outside the 10s window).");
        Map<String, SnapshotEntry> snapshotForRun3 = snapshotForRun2; // We use the state after RUN 1
        Map<String, Object> context3 = createDataContext("{\"temperature\": 21.0, \"humidity\": 55}", snapshotForRun3);

        injectCurrentTime(configNode, timeRun3);
        boolean result3 = evaluator.evaluateGraph(graph, context3);

        System.out.println("[RUN 3] RESULT: Transformation was NOT triggered (false).");
        // ACCEPTANCE CRITERIA 3: Transformation is NOT triggered when a change is outside the window
        assertFalse(result3, "Should be false since the first change is now outside the window.");
        System.out.println("✓ Acceptance Criteria 2 met: Transformation NOT triggered for change outside time window.");
    }

    // =====================================================================================
    // NEW TEST CASE FOR CHANGE DETECTION
    // =====================================================================================

    @Test
    public void testChangeDetection_OrMode() throws IOException, GraphEvaluationException {
        // Test: Trigger if temperature OR humidity changes (OR-Mode)
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ProviderNode humidityProvider = createProviderNode("source.sensor.humidity", 1);

        ConfigNode configNode = createConfigNode(2, tempProvider, humidityProvider);
        configNode.setMode(ConfigNode.ChangeDetectionMode.OR);

        FinalNode finalNode = createFinalNode(3, configNode);

        List<Node> graph = Arrays.asList(tempProvider, humidityProvider, configNode, finalNode);

        // Test Case 1: First run (no snapshot), should detect change -> true
        Map<String, Object> context1 = createDataContext("{\"temperature\": 20.0, \"humidity\": 50}", null);
        boolean result1 = evaluator.evaluateGraph(graph, context1);
        assertTrue(result1, "Should detect change on first run (no snapshot)");
        assertNotNull(configNode.getNewSnapshotData(), "Should create a new snapshot on first run");

        // Test Case 2: Second run, no change -> false
        Map<String, SnapshotEntry> snapshotForRun2 = configNode.getNewSnapshotData();
        Map<String, Object> context2 = createDataContext("{\"temperature\": 20.0, \"humidity\": 50}", snapshotForRun2);
        boolean result2 = evaluator.evaluateGraph(graph, context2);
        assertFalse(result2, "Should not detect change when data is identical to snapshot");
    }

    @Test
    public void testChangeDetection_AndMode() throws IOException, GraphEvaluationException {
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
    }

    // ### ADDED TEST FOR BYPASS SWITCH ###
    @Test
    public void testChangeDetection_BypassSwitch() throws IOException, GraphEvaluationException {
        // Test: The bypass switch on the ConfigNode disables the check
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ConfigNode configNode = createConfigNode(1, tempProvider);
        configNode.setActive(false); // Bypass is ACTIVE (change detection is OFF)
        FinalNode finalNode = createFinalNode(2, configNode);
        List<Node> graph = Arrays.asList(tempProvider, configNode, finalNode);

        // Snapshot says the old value was 20.0
        Map<String, SnapshotEntry> snapshot = Map.of("source.sensor.temperature", new SnapshotEntry(20.0, 0L));
        // The new value is 25.0 - a clear change
        Map<String, Object> context = createDataContext("{\"temperature\": 25.0}", snapshot);

        // Despite the change, the result must be false because the bypass is active
        assertFalse(evaluator.evaluateGraph(graph, context), "Should be false when bypass is active, even if data changed");
    }

    @Test
    public void testChangeDetection_CombinedWithOperator() throws IOException, GraphEvaluationException {
        // Test Logic: Trigger when (temperature changes) OR (status is "ERROR")

        // --- Path A: Change Detection ---
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ConfigNode configNode = createConfigNode(1, tempProvider); // Monitors only temperature
        configNode.setActive(true); // Ensure change detection is active

        // --- Path B: Operator Logic ---
        ProviderNode statusProvider = createProviderNode("source.sensor.status", 2);
        ConstantNode errorStatus = createConstantNode("ErrorStatus", "ERROR", 3);
        LogicNode statusCheck = createLogicNode("StatusCheckIsError", LogicOperator.EQUALS, 4, statusProvider, errorStatus);

        // --- Combination ---
        LogicNode combinedOr = createLogicNode("CombinedOr", LogicOperator.OR, 5, configNode, statusCheck);
        FinalNode finalNode = createFinalNode(6, combinedOr);

        List<Node> graph = Arrays.asList(tempProvider, configNode, statusProvider, errorStatus, statusCheck, combinedOr, finalNode);

        // --- Test Cases ---

        // Case 1: No change, Status OK -> false
        Map<String, SnapshotEntry> snapshot1 = Map.of("source.sensor.temperature", new SnapshotEntry(20.0, 0L));
        Map<String, Object> context1 = createDataContext("{\"temperature\": 20.0, \"status\": \"OK\"}", snapshot1);
        boolean result1 = evaluator.evaluateGraph(graph, context1);
        assertFalse(result1, "Should be false when no change and status is OK");

        // Case 2: Temperature changes, Status OK -> true
        Map<String, SnapshotEntry> snapshot2 = configNode.getNewSnapshotData();
        Map<String, Object> context2 = createDataContext("{\"temperature\": 21.0, \"status\": \"OK\"}", snapshot2);
        boolean result2 = evaluator.evaluateGraph(graph, context2);
        assertTrue(result2, "Should be true when temperature changes, even if status is OK");

        // Case 3: No change, Status is ERROR -> true
        Map<String, SnapshotEntry> snapshot3 = configNode.getNewSnapshotData();
        Map<String, Object> context3 = createDataContext("{\"temperature\": 21.0, \"status\": \"ERROR\"}", snapshot3);
        boolean result3 = evaluator.evaluateGraph(graph, context3);
        assertTrue(result3, "Should be true when status is ERROR, even if temperature has not changed");

        // Case 4: Temperature changes AND Status is ERROR -> true
        Map<String, SnapshotEntry> snapshot4 = configNode.getNewSnapshotData();
        Map<String, Object> context4 = createDataContext("{\"temperature\": 22.0, \"status\": \"ERROR\"}", snapshot4);
        boolean result4 = evaluator.evaluateGraph(graph, context4);
        assertTrue(result4, "Should be true when both conditions are met");
    }

    // =====================================================================================
    // YOUR ORIGINAL TESTS, CORRECTED
    // =====================================================================================

    @Test
    public void testNumberPredicates_LessThan() throws IOException, GraphEvaluationException {
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ConstantNode threshold = createConstantNode("Threshold", 20.0, 1);
        LogicNode comparison = createLogicNode("TempCheck", LogicOperator.LESS_THAN, 2, tempProvider, threshold);
        FinalNode finalNode = createFinalNode(3, comparison);
        List<Node> graph = Arrays.asList(tempProvider, threshold, comparison, finalNode);

        Map<String, Object> context1 = createDataContext("{\"temperature\": 15.5}", null);
        assertTrue(evaluator.evaluateGraph(graph, context1), "15.5 should be less than 20.0");

        Map<String, Object> context2 = createDataContext("{\"temperature\": 25.0}", null);
        assertFalse(evaluator.evaluateGraph(graph, context2), "25.0 should not be less than 20.0");
    }

    @Test
    public void testNumberPredicates_Between() throws IOException, GraphEvaluationException {
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ConstantNode lowerBound = createConstantNode("LowerBound", 10.0, 1);
        ConstantNode upperBound = createConstantNode("UpperBound", 30.0, 2);
        LogicNode betweenCheck = createLogicNode("BetweenCheck", LogicOperator.BETWEEN, 3, tempProvider, lowerBound, upperBound);
        FinalNode finalNode = createFinalNode(4, betweenCheck);
        List<Node> graph = Arrays.asList(tempProvider, lowerBound, upperBound, betweenCheck, finalNode);

        Map<String, Object> context1 = createDataContext("{\"temperature\": 20.0}", null);
        assertTrue(evaluator.evaluateGraph(graph, context1), "20.0 should be between 10.0 and 30.0");

        Map<String, Object> context2 = createDataContext("{\"temperature\": 5.0}", null);
        assertFalse(evaluator.evaluateGraph(graph, context2), "5.0 should not be between 10.0 and 30.0");

        Map<String, Object> context3 = createDataContext("{\"temperature\": 35.0}", null);
        assertFalse(evaluator.evaluateGraph(graph, context3), "35.0 should not be between 10.0 and 30.0");
    }

    @Test
    public void testBooleanPredicates() throws IOException, GraphEvaluationException {
        ProviderNode activeProvider = createProviderNode("source.sensor.isActive", 0);
        ProviderNode maintenanceProvider = createProviderNode("source.sensor.maintenanceMode", 1);
        LogicNode activeCheck = createLogicNode("ActiveCheck", LogicOperator.IS_TRUE, 2, activeProvider);
        LogicNode maintenanceCheck = createLogicNode("MaintenanceCheck", LogicOperator.IS_FALSE, 3, maintenanceProvider);
        LogicNode finalAnd = createLogicNode("FinalCheck", LogicOperator.AND, 4, activeCheck, maintenanceCheck);
        FinalNode finalNode = createFinalNode(5, finalAnd);
        List<Node> graph = Arrays.asList(activeProvider, maintenanceProvider, activeCheck, maintenanceCheck, finalAnd, finalNode);

        Map<String, Object> context1 = createDataContext("{\"isActive\": true, \"maintenanceMode\": false}", null);
        assertTrue(evaluator.evaluateGraph(graph, context1), "Should be true when active and not in maintenance");

        Map<String, Object> context2 = createDataContext("{\"isActive\": false, \"maintenanceMode\": false}", null);
        assertFalse(evaluator.evaluateGraph(graph, context2), "Should be false when not active");

        Map<String, Object> context3 = createDataContext("{\"isActive\": true, \"maintenanceMode\": true}", null);
        assertFalse(evaluator.evaluateGraph(graph, context3), "Should be false when in maintenance mode");
    }

    @Test
    public void testStringPredicates() throws IOException, GraphEvaluationException {
        ProviderNode deviceNameProvider = createProviderNode("source.sensor.deviceName", 0);
        ProviderNode statusProvider = createProviderNode("source.sensor.status", 1);
        ConstantNode sensorPattern = createConstantNode("SensorPattern", "Sensor", 2);
        ConstantNode okStatus = createConstantNode("OKStatus", "OK", 3);
        LogicNode nameCheck = createLogicNode("NameCheck", LogicOperator.STRING_CONTAINS, 4, deviceNameProvider, sensorPattern);
        LogicNode statusCheck = createLogicNode("StatusCheck", LogicOperator.EQUALS, 5, statusProvider, okStatus);
        LogicNode finalAnd = createLogicNode("FinalCheck", LogicOperator.AND, 6, nameCheck, statusCheck);
        FinalNode finalNode = createFinalNode(7, finalAnd);
        List<Node> graph = Arrays.asList(deviceNameProvider, statusProvider, sensorPattern, okStatus, nameCheck, statusCheck, finalAnd, finalNode);

        Map<String, Object> context1 = createDataContext("{\"deviceName\": \"TemperatureSensor\", \"status\": \"OK\"}", null);
        assertTrue(evaluator.evaluateGraph(graph, context1), "Should be true for valid sensor with OK status");

        Map<String, Object> context2 = createDataContext("{\"deviceName\": \"Thermometer\", \"status\": \"OK\"}", null);
        assertFalse(evaluator.evaluateGraph(graph, context2), "Should be false for non-sensor device");
    }

    @Test
    public void testComplexLogicalOperators() throws IOException, GraphEvaluationException {
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
        List<Node> graph = Arrays.asList(tempProvider, humidityProvider, maintenanceProvider, tempThreshold, humidityThreshold, tempCheck, humidityCheck, tempOrHumidity, notMaintenance, finalAnd, finalNode);

        Map<String, Object> context1 = createDataContext("{\"temperature\": 30.0, \"humidity\": 60.0, \"maintenanceMode\": false}", null);
        assertTrue(evaluator.evaluateGraph(graph, context1), "Should be true for high temp and not in maintenance");
    }

    @Test
    public void testArrayPredicates() throws IOException, GraphEvaluationException {
        ProviderNode sensorsProvider = createProviderNode("source.sensor.activeSensors", 0);
        ConstantNode temperatureSensorName = createConstantNode("TempSensorName", "temperature", 1);
        ConstantNode minSensorCount = createConstantNode("MinSensorCount", 2, 2);
        LogicNode containsCheck = createLogicNode("ContainsTemperatureSensor", LogicOperator.CONTAINS_ELEMENT, 3, sensorsProvider, temperatureSensorName);
        LogicNode lengthCheck = createLogicNode("SufficientSensors", LogicOperator.LENGTH_GT, 4, sensorsProvider, minSensorCount);
        LogicNode finalAnd = createLogicNode("FinalCheck", LogicOperator.AND, 5, containsCheck, lengthCheck);
        FinalNode finalNode = createFinalNode(6, finalAnd);
        List<Node> graph = Arrays.asList(sensorsProvider, temperatureSensorName, minSensorCount, containsCheck, lengthCheck, finalAnd, finalNode);

        Map<String, Object> context1 = createDataContext("{\"activeSensors\": [\"temperature\", \"humidity\", \"pressure\"]}", null);
        assertTrue(evaluator.evaluateGraph(graph, context1), "Should be true when temperature sensor is present and count >= 2");

        Map<String, Object> context2 = createDataContext("{\"activeSensors\": [\"humidity\", \"pressure\"]}", null);
        assertFalse(evaluator.evaluateGraph(graph, context2), "Should be false when temperature sensor is missing");

        Map<String, Object> context3 = createDataContext("{\"activeSensors\": [\"temperature\"]}", null);
        assertFalse(evaluator.evaluateGraph(graph, context3), "Should be false when sensor count is insufficient");
    }

    @Test
    public void testObjectPredicates() throws IOException, GraphEvaluationException {
        ProviderNode configProvider = createProviderNode("source.sensor.config", 0);
        ConstantNode requiredKeys = createConstantNode("RequiredKeys", Arrays.asList("threshold", "interval"), 1);
        ConstantNode thresholdKey = createConstantNode("ThresholdKey", "threshold", 2);
        LogicNode hasAllKeysCheck = createLogicNode("HasAllRequiredKeys", LogicOperator.HAS_ALL_KEYS, 3, configProvider, requiredKeys);
        LogicNode hasThresholdCheck = createLogicNode("HasThreshold", LogicOperator.HAS_KEY, 4, configProvider, thresholdKey);
        LogicNode finalAnd = createLogicNode("FinalCheck", LogicOperator.AND, 5, hasAllKeysCheck, hasThresholdCheck);
        FinalNode finalNode = createFinalNode(6, finalAnd);
        List<Node> graph = Arrays.asList(configProvider, requiredKeys, thresholdKey, hasAllKeysCheck, hasThresholdCheck, finalAnd, finalNode);

        Map<String, Object> context1 = createDataContext("{\"config\": {\"threshold\": 25.0, \"interval\": 5000, \"name\": \"TempSensor\"}}", null);
        assertTrue(evaluator.evaluateGraph(graph, context1), "Should be true when all required keys are present");

        Map<String, Object> context2 = createDataContext("{\"config\": {\"threshold\": 25.0, \"name\": \"TempSensor\"}}", null);
        assertFalse(evaluator.evaluateGraph(graph, context2), "Should be false when interval key is missing");
    }

    @Test
    public void testExistencePredicates() throws IOException, GraphEvaluationException {
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

        Map<String, Object> context1 = createDataContext("{\"temperature\": 25.0, \"pressure\": 1013.25}", null);
        assertTrue(evaluator.evaluateGraph(graph, context1), "Should be true when temp and pressure exist, nonexistent doesn't");

        Map<String, Object> context2 = createDataContext("{\"temperature\": 25.0}", null);
        assertFalse(evaluator.evaluateGraph(graph, context2), "Should be false when pressure is missing");
    }

    // =====================================================================================
    // HELPER METHODS
    // =====================================================================================

    private void injectCurrentTime(ConfigNode node, long timeMillis) {
        // This call assumes you have added the `setTestTime` method to your ConfigNode class.
        node.setTestTime(timeMillis);
    }

    private ProviderNode createProviderNode(String jsonPath, int id) {
        try {
            ProviderNode node = new ProviderNode(jsonPath);
            node.setId(id);
            return node;
        } catch (NodeConfigurationException e) {
            throw new RuntimeException("Failed to create ProviderNode: " + jsonPath, e);
        }
    }

    private ConstantNode createConstantNode(String name, Object value, int id) {
        try {
            ConstantNode node = new ConstantNode(name, value);
            node.setId(id);
            return node;
        } catch (NodeConfigurationException e) {
            throw new RuntimeException("Failed to create ConstantNode: " + name, e);
        }
    }

    private LogicNode createLogicNode(String name, LogicOperator operator, int id, Node... inputs) {
        try {
            LogicNode node = new LogicNode(name, operator);
            node.setInputNodes(Arrays.asList(inputs));
            node.setId(id);
            return node;
        } catch (NodeConfigurationException e) {
            throw new RuntimeException("Failed to create LogicNode: " + name, e);
        }
    }

    private FinalNode createFinalNode(int id, Node input) {
        try {
            FinalNode node = new FinalNode();
            node.setId(id);
            node.setInputNodes(List.of(input));
            return node;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create FinalNode", e);
        }
    }

    private ConfigNode createConfigNode(int id, Node... inputs) {
        ConfigNode node = new ConfigNode();
        node.setId(id);
        node.setInputNodes(Arrays.asList(inputs));
        return node;
    }
}