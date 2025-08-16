package de.unistuttgart.stayinsync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.unistuttgart.stayinsync.syncnode.domain.TransformJob;
import de.unistuttgart.stayinsync.syncnode.logic_engine.LogicGraphEvaluator;
import de.unistuttgart.stayinsync.syncnode.logic_engine.LogicGraphEvaluator.EvaluationResult;
import de.unistuttgart.stayinsync.transport.exception.GraphEvaluationException;
import de.unistuttgart.stayinsync.transport.exception.NodeConfigurationException;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.LogicOperator;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.*;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.util.GraphTopologicalSorter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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
    private final TransformJob dummyJob = new TransformJob("test-transform", "job-123", "script-1", "", "js", "", Collections.emptyMap());

    @BeforeEach
    void setUp() {
        GraphTopologicalSorter sorter = new GraphTopologicalSorter();
        evaluator = new LogicGraphEvaluator();
        evaluator.setSorter(sorter);
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

    // =====================================================================================
    // NEUER TESTFALL FÜR DIE CHANGE DETECTION
    // =====================================================================================

    @Test
    void testChangeDetection_OrMode() throws IOException, GraphEvaluationException {
        // Test: Trigger, if temperature OR humidity changes (OR-Mode)
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ProviderNode humidityProvider = createProviderNode("source.sensor.humidity", 1);

        ConfigNode configNode = createConfigNode(2, tempProvider, humidityProvider);
        configNode.setMode(ConfigNode.ChangeDetectionMode.OR);

        FinalNode finalNode = createFinalNode(3, configNode);

        List<Node> graph = Arrays.asList(tempProvider, humidityProvider, configNode, finalNode);

        // Testfall 1: Erster Lauf (kein Snapshot), sollte Änderung erkennen -> true
        Map<String, JsonNode> context1 = createDataContext("{\"temperature\": 20.0, \"humidity\": 50}", null);
        EvaluationResult result1 = evaluator.evaluateGraph(graph, context1, dummyJob);
        assertTrue(result1.finalResult(), "Should detect change on first run (no snapshot)");
        assertNotNull(result1.newSnapshot(), "Should create a new snapshot on first run");

        // Testfall 2: Zweiter Lauf, keine Änderung -> false
        Map<String, SnapshotEntry> snapshotForRun2 = result1.newSnapshot();
        Map<String, JsonNode> context2 = createDataContext("{\"temperature\": 20.0, \"humidity\": 50}", snapshotForRun2);
        EvaluationResult result2 = evaluator.evaluateGraph(graph, context2, dummyJob);
        assertFalse(result2.finalResult(), "Should not detect change when data is identical to snapshot");
    }

    @Test
    void testChangeDetection_CombinedWithOperator() throws IOException, GraphEvaluationException {
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
        EvaluationResult result1 = evaluator.evaluateGraph(graph, context1, dummyJob);
        assertFalse(result1.finalResult(), "Should be false when no change and status is OK");

        // Fall 2: Temperatur ändert sich, Status OK -> true
        Map<String, SnapshotEntry> snapshot2 = result1.newSnapshot();
        Map<String, JsonNode> context2 = createDataContext("{\"temperature\": 21.0, \"status\": \"OK\"}", snapshot2);
        EvaluationResult result2 = evaluator.evaluateGraph(graph, context2, dummyJob);
        assertTrue(result2.finalResult(), "Should be true when temperature changes, even if status is OK");

        // Fall 3: Keine Änderung, Status ist ERROR -> true
        Map<String, SnapshotEntry> snapshot3 = result2.newSnapshot();
        Map<String, JsonNode> context3 = createDataContext("{\"temperature\": 21.0, \"status\": \"ERROR\"}", snapshot3);
        EvaluationResult result3 = evaluator.evaluateGraph(graph, context3, dummyJob);
        assertTrue(result3.finalResult(), "Should be true when status is ERROR, even if temperature has not changed");

        // Fall 4: Temperatur ändert sich UND Status ist ERROR -> true
        Map<String, SnapshotEntry> snapshot4 = result3.newSnapshot();
        Map<String, JsonNode> context4 = createDataContext("{\"temperature\": 22.0, \"status\": \"ERROR\"}", snapshot4);
        EvaluationResult result4 = evaluator.evaluateGraph(graph, context4, dummyJob);
        assertTrue(result4.finalResult(), "Should be true when both conditions are met");
    }

    // =====================================================================================
    // DEINE URSPRÜNGLICHEN TESTS, KORRIGIERT
    // =====================================================================================

    @Test
    void testNumberPredicates_LessThan() throws IOException, GraphEvaluationException {
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ConstantNode threshold = createConstantNode("Threshold", 20.0, 1);
        LogicNode comparison = createLogicNode("TempCheck", LogicOperator.LESS_THAN, 2, tempProvider, threshold);
        FinalNode finalNode = createFinalNode(3, comparison);
        List<Node> graph = Arrays.asList(tempProvider, threshold, comparison, finalNode);

        Map<String, JsonNode> context1 = createDataContext("{\"temperature\": 15.5}", null);
        assertTrue(evaluator.evaluateGraph(graph, context1, dummyJob).finalResult(), "15.5 should be less than 20.0");

        Map<String, JsonNode> context2 = createDataContext("{\"temperature\": 25.0}", null);
        assertFalse(evaluator.evaluateGraph(graph, context2, dummyJob).finalResult(), "25.0 should not be less than 20.0");
    }

    @Test
    void testNumberPredicates_Between() throws IOException, GraphEvaluationException {
        ProviderNode tempProvider = createProviderNode("source.sensor.temperature", 0);
        ConstantNode lowerBound = createConstantNode("LowerBound", 10.0, 1);
        ConstantNode upperBound = createConstantNode("UpperBound", 30.0, 2);
        LogicNode betweenCheck = createLogicNode("BetweenCheck", LogicOperator.BETWEEN, 3, tempProvider, lowerBound, upperBound);
        FinalNode finalNode = createFinalNode(4, betweenCheck);
        List<Node> graph = Arrays.asList(tempProvider, lowerBound, upperBound, betweenCheck, finalNode);

        Map<String, JsonNode> context1 = createDataContext("{\"temperature\": 20.0}", null);
        assertTrue(evaluator.evaluateGraph(graph, context1, dummyJob).finalResult(), "20.0 should be between 10.0 and 30.0");

        Map<String, JsonNode> context2 = createDataContext("{\"temperature\": 5.0}", null);
        assertFalse(evaluator.evaluateGraph(graph, context2, dummyJob).finalResult(), "5.0 should not be between 10.0 and 30.0");

        Map<String, JsonNode> context3 = createDataContext("{\"temperature\": 35.0}", null);
        assertFalse(evaluator.evaluateGraph(graph, context3, dummyJob).finalResult(), "35.0 should not be between 10.0 and 30.0");
    }

    @Test
    void testBooleanPredicates() throws IOException, GraphEvaluationException {
        ProviderNode activeProvider = createProviderNode("source.sensor.isActive", 0);
        ProviderNode maintenanceProvider = createProviderNode("source.sensor.maintenanceMode", 1);
        LogicNode activeCheck = createLogicNode("ActiveCheck", LogicOperator.IS_TRUE, 2, activeProvider);
        LogicNode maintenanceCheck = createLogicNode("MaintenanceCheck", LogicOperator.IS_FALSE, 3, maintenanceProvider);
        LogicNode finalAnd = createLogicNode("FinalCheck", LogicOperator.AND, 4, activeCheck, maintenanceCheck);
        FinalNode finalNode = createFinalNode(5, finalAnd);
        List<Node> graph = Arrays.asList(activeProvider, maintenanceProvider, activeCheck, maintenanceCheck, finalAnd, finalNode);

        Map<String, JsonNode> context1 = createDataContext("{\"isActive\": true, \"maintenanceMode\": false}", null);
        assertTrue(evaluator.evaluateGraph(graph, context1, dummyJob).finalResult(), "Should be true when active and not in maintenance");

        Map<String, JsonNode> context2 = createDataContext("{\"isActive\": false, \"maintenanceMode\": false}", null);
        assertFalse(evaluator.evaluateGraph(graph, context2, dummyJob).finalResult(), "Should be false when not active");

        Map<String, JsonNode> context3 = createDataContext("{\"isActive\": true, \"maintenanceMode\": true}", null);
        assertFalse(evaluator.evaluateGraph(graph, context3, dummyJob).finalResult(), "Should be false when in maintenance mode");
    }

    @Test
    void testStringPredicates() throws IOException, GraphEvaluationException {
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
        assertTrue(evaluator.evaluateGraph(graph, context1, dummyJob).finalResult(), "Should be true for valid sensor with OK status");

        Map<String, JsonNode> context2 = createDataContext("{\"deviceName\": \"Thermometer\", \"status\": \"OK\"}", null);
        assertFalse(evaluator.evaluateGraph(graph, context2, dummyJob).finalResult(), "Should be false for non-sensor device");
    }

    @Test
    void testComplexLogicalOperators() throws IOException, GraphEvaluationException {
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
        assertTrue(evaluator.evaluateGraph(graph, context1, dummyJob).finalResult(), "Should be true for high temp and not in maintenance");
    }

    // ... Die restlichen deiner ursprünglichen Tests (`testArrayPredicates`, etc.) fügst du hier nach demselben korrigierten Muster ein.

    // =====================================================================================
    // HELFER-METHODEN
    // =====================================================================================

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