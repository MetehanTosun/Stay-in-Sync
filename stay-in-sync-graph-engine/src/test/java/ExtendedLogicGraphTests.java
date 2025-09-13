import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.exception.NodeConfigurationException;
import de.unistuttgart.graphengine.logic_engine.LogicGraphEvaluator;
import de.unistuttgart.graphengine.logic_operator.LogicOperator;
import de.unistuttgart.graphengine.nodes.*;
import de.unistuttgart.graphengine.util.GraphTopologicalSorter;
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

    private Map<String, JsonNode> createDataContext(String json, Map<String, SnapshotEntry> snapshot) throws IOException {
        JsonNode data = objectMapper.readTree(json);
        ObjectNode sourceNode = objectMapper.createObjectNode();
        sourceNode.set("sensor", data);

        Map<String, JsonNode> dataContext = new HashMap<>();
        dataContext.put("source", sourceNode);

        if (snapshot != null) {
            dataContext.put("__snapshot", objectMapper.valueToTree(snapshot));
        }
        return dataContext;
    }

    @Test
    public void testChangeDetection_WithTimeWindow() throws IOException, GraphEvaluationException {
        System.out.println("--- TEST: Zeitfenster-Feature ('Sliding Window') ---");

        // ARRANGE
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ProviderNode humidityProvider = createProviderNode("source.sensor.humidity", 1);

        // AKZEPTANZKRITERIUM 1: User kann Zeitfenster aktivieren und Dauer einstellen
        ConfigNode configNode = createConfigNode(2, tempProvider, humidityProvider);
        configNode.setMode(ConfigNode.ChangeDetectionMode.AND);
        configNode.setTimeWindowEnabled(true);
        configNode.setTimeWindowMillis(10000); // 10 Sekunden
        System.out.println("[SETUP] Akzeptanzkriterium 1: ConfigNode erstellt. Modus=AND, Zeitfenster=10s aktiviert.");

        FinalNode finalNode = createFinalNode(3, configNode);
        List<Node> graph = Arrays.asList(tempProvider, humidityProvider, configNode, finalNode);

        // --- Zeit-Simulation ---
        long timeRun1 = System.currentTimeMillis();
        long timeRun2 = timeRun1 + 5000;  // 5 Sekunden nach Lauf 1
        long timeRun3 = timeRun1 + 12000; // 12 Sekunden nach Lauf 1

        // === RUN 1: Eine von zwei Änderungen findet statt ===
        System.out.println("\n[RUN 1] Situation: Nur die Temperatur ändert sich.");
        Map<String, SnapshotEntry> initialSnapshot = Map.of(
                "source.sensor.temperature", new SnapshotEntry(20.0, timeRun1 - 20000),
                "source.sensor.humidity", new SnapshotEntry(50, timeRun1 - 20000)
        );
        Map<String, JsonNode> context1 = createDataContext("{\"temperature\": 21.0, \"humidity\": 50}", initialSnapshot);

        injectCurrentTime(configNode, timeRun1);
        LogicGraphEvaluator.EvaluationResult result1 = evaluator.evaluateGraph(graph, context1);

        System.out.println("[RUN 1] ERGEBNIS: Transformation wurde NICHT ausgelöst (false).");
        assertFalse(result1.finalResult(), "Sollte false sein, da nur ein Wert geändert wurde.");


        // === RUN 2: Zweite Änderung INNERHALB des Fensters ===
        System.out.println("\n[RUN 2] Situation: Zweite Änderung erfolgt 5s später (innerhalb des 10s-Fensters).");
        Map<String, SnapshotEntry> snapshotForRun2 = result1.newSnapshot();
        Map<String, JsonNode> context2 = createDataContext("{\"temperature\": 21.0, \"humidity\": 55}", snapshotForRun2);

        injectCurrentTime(configNode, timeRun2);
        LogicGraphEvaluator.EvaluationResult result2 = evaluator.evaluateGraph(graph, context2);

        System.out.println("[RUN 2] ERGEBNIS: Transformation WURDE ausgelöst (true).");
        // AKZEPTANZKRITERIUM 2: Transformation wird ausgelöst, wenn alle Änderungen im Fenster liegen
        assertTrue(result2.finalResult(), "Sollte true sein, da beide Änderungen innerhalb des Fensters stattfanden.");
        System.out.println("✅ Akzeptanzkriterium 1 erfüllt: Transformation bei Änderungen im Zeitfenster ausgelöst.");


        // === RUN 3: Zweite Änderung AUSSERHALB des Fensters ===
        System.out.println("\n[RUN 3] Situation: Zweite Änderung erfolgt 12s nach der ersten (außerhalb des 10s-Fensters).");
        Map<String, SnapshotEntry> snapshotForRun3 = result1.newSnapshot(); // Wir gehen vom Zustand nach RUN 1 aus
        Map<String, JsonNode> context3 = createDataContext("{\"temperature\": 21.0, \"humidity\": 55}", snapshotForRun3);

        injectCurrentTime(configNode, timeRun3);
        LogicGraphEvaluator.EvaluationResult result3 = evaluator.evaluateGraph(graph, context3);

        System.out.println("[RUN 3] ERGEBNIS: Transformation wurde NICHT ausgelöst (false).");
        // AKZEPTANZKRITERIUM 3: Transformation wird NICHT ausgelöst, wenn eine Änderung außerhalb des Fensters liegt
        assertFalse(result3.finalResult(), "Sollte false sein, da die erste Änderung nun außerhalb des Fensters liegt.");
        System.out.println("✅ Akzeptanzkriterium 2 erfüllt: Transformation bei Änderung außerhalb des Zeitfensters NICHT ausgelöst.");
    }

    // =====================================================================================
    // NEUER TESTFALL FÜR DIE CHANGE DETECTION
    // =====================================================================================

    @Test
    public void testChangeDetection_OrMode() throws IOException, GraphEvaluationException {
        // Test: Trigger, if temperature OR humidity changes (OR-Mode)
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ProviderNode humidityProvider = createProviderNode("source.sensor.humidity", 1);

        ConfigNode configNode = createConfigNode(2, tempProvider, humidityProvider);
        configNode.setMode(ConfigNode.ChangeDetectionMode.OR);

        FinalNode finalNode = createFinalNode(3, configNode);

        List<Node> graph = Arrays.asList(tempProvider, humidityProvider, configNode, finalNode);

        // Testfall 1: Erster Lauf (kein Snapshot), sollte Änderung erkennen -> true
        Map<String, JsonNode> context1 = createDataContext("{\"temperature\": 20.0, \"humidity\": 50}", null);
        LogicGraphEvaluator.EvaluationResult result1 = evaluator.evaluateGraph(graph, context1);
        assertTrue(result1.finalResult(), "Should detect change on first run (no snapshot)");
        assertNotNull(result1.newSnapshot(), "Should create a new snapshot on first run");

        // Testfall 2: Zweiter Lauf, keine Änderung -> false
        Map<String, SnapshotEntry> snapshotForRun2 = result1.newSnapshot();
        Map<String, JsonNode> context2 = createDataContext("{\"temperature\": 20.0, \"humidity\": 50}", snapshotForRun2);
        LogicGraphEvaluator.EvaluationResult result2 = evaluator.evaluateGraph(graph, context2);
        assertFalse(result2.finalResult(), "Should not detect change when data is identical to snapshot");
    }

    @Test
    public void testChangeDetection_AndMode() throws IOException, GraphEvaluationException {
        // Test: Trigger, ONLY if temperature AND humidity change (AND-Mode)
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

        // Fall 1: Nur eine Variable ändert sich -> false
        Map<String, JsonNode> context1 = createDataContext("{\"temperature\": 21.0, \"humidity\": 50}", snapshot);
        assertFalse(evaluator.evaluateGraph(graph, context1).finalResult(), "Should be false if only one value changes in AND mode");

        // Fall 2: Keine Variable ändert sich -> false
        Map<String, JsonNode> context2 = createDataContext("{\"temperature\": 20.0, \"humidity\": 50}", snapshot);
        assertFalse(evaluator.evaluateGraph(graph, context2).finalResult(), "Should be false if no value changes in AND mode");

        // Fall 3: Beide Variablen ändern sich -> true
        Map<String, JsonNode> context3 = createDataContext("{\"temperature\": 21.0, \"humidity\": 51}", snapshot);
        assertTrue(evaluator.evaluateGraph(graph, context3).finalResult(), "Should be true if both values change in AND mode");
    }

    // ### HINZUGEFÜGTER TEST FÜR BYPASS-SCHALTER ###
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
        Map<String, JsonNode> context = createDataContext("{\"temperature\": 25.0}", snapshot);

        // Despite the change, the result must be false because the bypass is active
        assertFalse(evaluator.evaluateGraph(graph, context).finalResult(), "Should be false when bypass is active, even if data changed");
    }

    @Test
    public void testChangeDetection_CombinedWithOperator() throws IOException, GraphEvaluationException {
        // Test-Logik: Trigger, wenn (sich die Temperatur ändert) ODER (der Status "ERROR" ist)

        // --- Pfad A: Change Detection ---
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ConfigNode configNode = createConfigNode(1, tempProvider); // Überwacht nur die Temperatur
        configNode.setActive(true); // Sicherstellen, dass Change Detection aktiv ist

        // --- Pfad B: Operator-Logik ---
        ProviderNode statusProvider = createProviderNode("source.sensor.status", 2);
        ConstantNode errorStatus = createConstantNode("ErrorStatus", "ERROR", 3);
        LogicNode statusCheck = createLogicNode("StatusCheckIsError", LogicOperator.EQUALS, 4, statusProvider, errorStatus);

        // --- Verknüpfung ---
        LogicNode combinedOr = createLogicNode("CombinedOr", LogicOperator.OR, 5, configNode, statusCheck);
        FinalNode finalNode = createFinalNode(6, combinedOr);

        List<Node> graph = Arrays.asList(tempProvider, configNode, statusProvider, errorStatus, statusCheck, combinedOr, finalNode);

        // --- Testfälle ---

        // Fall 1: Keine Änderung, Status OK -> false
        Map<String, SnapshotEntry> snapshot1 = Map.of("source.sensor.temperature", new SnapshotEntry(20.0, 0L));
        Map<String, JsonNode> context1 = createDataContext("{\"temperature\": 20.0, \"status\": \"OK\"}", snapshot1);
        LogicGraphEvaluator.EvaluationResult result1 = evaluator.evaluateGraph(graph, context1);
        assertFalse(result1.finalResult(), "Should be false when no change and status is OK");

        // Fall 2: Temperatur ändert sich, Status OK -> true
        Map<String, SnapshotEntry> snapshot2 = result1.newSnapshot();
        Map<String, JsonNode> context2 = createDataContext("{\"temperature\": 21.0, \"status\": \"OK\"}", snapshot2);
        LogicGraphEvaluator.EvaluationResult result2 = evaluator.evaluateGraph(graph, context2);
        assertTrue(result2.finalResult(), "Should be true when temperature changes, even if status is OK");

        // Fall 3: Keine Änderung, Status ist ERROR -> true
        Map<String, SnapshotEntry> snapshot3 = result2.newSnapshot();
        Map<String, JsonNode> context3 = createDataContext("{\"temperature\": 21.0, \"status\": \"ERROR\"}", snapshot3);
        LogicGraphEvaluator.EvaluationResult result3 = evaluator.evaluateGraph(graph, context3);
        assertTrue(result3.finalResult(), "Should be true when status is ERROR, even if temperature has not changed");

        // Fall 4: Temperatur ändert sich UND Status ist ERROR -> true
        Map<String, SnapshotEntry> snapshot4 = result3.newSnapshot();
        Map<String, JsonNode> context4 = createDataContext("{\"temperature\": 22.0, \"status\": \"ERROR\"}", snapshot4);
        LogicGraphEvaluator.EvaluationResult result4 = evaluator.evaluateGraph(graph, context4);
        assertTrue(result4.finalResult(), "Should be true when both conditions are met");
    }

    // =====================================================================================
    // DEINE URSPRÜNGLICHEN TESTS, KORRIGIERT
    // =====================================================================================

    @Test
    public void testNumberPredicates_LessThan() throws IOException, GraphEvaluationException {
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ConstantNode threshold = createConstantNode("Threshold", 20.0, 1);
        LogicNode comparison = createLogicNode("TempCheck", LogicOperator.LESS_THAN, 2, tempProvider, threshold);
        FinalNode finalNode = createFinalNode(3, comparison);
        List<Node> graph = Arrays.asList(tempProvider, threshold, comparison, finalNode);

        Map<String, JsonNode> context1 = createDataContext("{\"temperature\": 15.5}", null);
        assertTrue(evaluator.evaluateGraph(graph, context1).finalResult(), "15.5 should be less than 20.0");

        Map<String, JsonNode> context2 = createDataContext("{\"temperature\": 25.0}", null);
        assertFalse(evaluator.evaluateGraph(graph, context2).finalResult(), "25.0 should not be less than 20.0");
    }

    @Test
    public void testNumberPredicates_Between() throws IOException, GraphEvaluationException {
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ConstantNode lowerBound = createConstantNode("LowerBound", 10.0, 1);
        ConstantNode upperBound = createConstantNode("UpperBound", 30.0, 2);
        LogicNode betweenCheck = createLogicNode("BetweenCheck", LogicOperator.BETWEEN, 3, tempProvider, lowerBound, upperBound);
        FinalNode finalNode = createFinalNode(4, betweenCheck);
        List<Node> graph = Arrays.asList(tempProvider, lowerBound, upperBound, betweenCheck, finalNode);

        Map<String, JsonNode> context1 = createDataContext("{\"temperature\": 20.0}", null);
        assertTrue(evaluator.evaluateGraph(graph, context1).finalResult(), "20.0 should be between 10.0 and 30.0");

        Map<String, JsonNode> context2 = createDataContext("{\"temperature\": 5.0}", null);
        assertFalse(evaluator.evaluateGraph(graph, context2).finalResult(), "5.0 should not be between 10.0 and 30.0");

        Map<String, JsonNode> context3 = createDataContext("{\"temperature\": 35.0}", null);
        assertFalse(evaluator.evaluateGraph(graph, context3).finalResult(), "35.0 should not be between 10.0 and 30.0");
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

        Map<String, JsonNode> context1 = createDataContext("{\"isActive\": true, \"maintenanceMode\": false}", null);
        assertTrue(evaluator.evaluateGraph(graph, context1).finalResult(), "Should be true when active and not in maintenance");

        Map<String, JsonNode> context2 = createDataContext("{\"isActive\": false, \"maintenanceMode\": false}", null);
        assertFalse(evaluator.evaluateGraph(graph, context2).finalResult(), "Should be false when not active");

        Map<String, JsonNode> context3 = createDataContext("{\"isActive\": true, \"maintenanceMode\": true}", null);
        assertFalse(evaluator.evaluateGraph(graph, context3).finalResult(), "Should be false when in maintenance mode");
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

        Map<String, JsonNode> context1 = createDataContext("{\"deviceName\": \"TemperatureSensor\", \"status\": \"OK\"}", null);
        assertTrue(evaluator.evaluateGraph(graph, context1).finalResult(), "Should be true for valid sensor with OK status");

        Map<String, JsonNode> context2 = createDataContext("{\"deviceName\": \"Thermometer\", \"status\": \"OK\"}", null);
        assertFalse(evaluator.evaluateGraph(graph, context2).finalResult(), "Should be false for non-sensor device");
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

        Map<String, JsonNode> context1 = createDataContext("{\"temperature\": 30.0, \"humidity\": 60.0, \"maintenanceMode\": false}", null);
        assertTrue(evaluator.evaluateGraph(graph, context1).finalResult(), "Should be true for high temp and not in maintenance");
    }

    // ... Die restlichen deiner ursprünglichen Tests (`testArrayPredicates`, etc.) fügst du hier nach demselben korrigierten Muster ein.

    // =====================================================================================
    // HELFER-METHODEN
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