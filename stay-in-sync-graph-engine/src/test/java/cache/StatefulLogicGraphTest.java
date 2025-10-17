package cache;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.cache.StatefulLogicGraph;
import de.unistuttgart.graphengine.exception.GraphConstructionException;
import de.unistuttgart.graphengine.nodes.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StatefulLogicGraph Tests")
public class StatefulLogicGraphTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    /**
     * Helper method to parse JSON without checked exception handling in tests
     */
    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse test JSON: " + json, e);
        }
    }

    @Test
    @DisplayName("should initialize with valid graph")
    void testValidInitialization() {
        List<Node> graph = createValidGraph();

        StatefulLogicGraph statefulGraph = new StatefulLogicGraph(graph);

        assertNotNull(statefulGraph);
        assertEquals(2, statefulGraph.getNodeCount());
        assertEquals(0, statefulGraph.getSnapshotSize());
    }

    @Test
    @DisplayName("should throw exception when graph is null")
    void testNullGraph() {
        GraphConstructionException exception = assertThrows(
                GraphConstructionException.class,
                () -> new StatefulLogicGraph(null)
        );

        assertEquals(GraphConstructionException.ErrorType.NULL_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Graph definition cannot be null"));
    }

    @Test
    @DisplayName("should throw exception when graph is empty")
    void testEmptyGraph() {
        GraphConstructionException exception = assertThrows(
                GraphConstructionException.class,
                () -> new StatefulLogicGraph(new ArrayList<>())
        );

        assertEquals(GraphConstructionException.ErrorType.EMPTY_GRAPH, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Graph definition cannot be empty"));
    }

    @Test
    @DisplayName("should throw exception when no ConfigNode found")
    void testMissingConfigNode() {
        ConstantNode constantNode = new ConstantNode("Test", 42);
        constantNode.setId(1);

        FinalNode finalNode = new FinalNode();
        finalNode.setId(0);
        finalNode.setInputNodes(Arrays.asList(constantNode));

        List<Node> graphWithoutConfig = Arrays.asList(constantNode, finalNode);

        GraphConstructionException exception = assertThrows(
                GraphConstructionException.class,
                () -> new StatefulLogicGraph(graphWithoutConfig)
        );

        assertEquals(GraphConstructionException.ErrorType.MISSING_REQUIRED_NODE, exception.getErrorType());
        assertTrue(exception.getMessage().contains("No ConfigNode found"));
    }

    @Test
    @DisplayName("should throw exception when multiple ConfigNodes found")
    void testMultipleConfigNodes() {
        ConfigNode config1 = new ConfigNode();
        config1.setId(1);

        ConfigNode config2 = new ConfigNode();
        config2.setId(2);

        FinalNode finalNode = new FinalNode();
        finalNode.setId(0);

        List<Node> graphWithTwoConfigs = Arrays.asList(config1, config2, finalNode);

        GraphConstructionException exception = assertThrows(
                GraphConstructionException.class,
                () -> new StatefulLogicGraph(graphWithTwoConfigs)
        );

        assertEquals(GraphConstructionException.ErrorType.DUPLICATE_NODE, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Found 2 ConfigNodes"));
    }

    @Test
    @DisplayName("should evaluate graph with provider nodes")
    void testEvaluateWithProviderNodes() {
        ProviderNode provider = new ProviderNode("source.system.value");
        provider.setId(2);

        ConfigNode configNode = new ConfigNode();
        configNode.setId(1);
        configNode.setInputNodes(Arrays.asList(provider));

        FinalNode finalNode = new FinalNode();
        finalNode.setId(0);
        finalNode.setInputNodes(Arrays.asList(configNode));

        List<Node> graph = Arrays.asList(provider, configNode, finalNode);
        StatefulLogicGraph statefulGraph = new StatefulLogicGraph(graph);

        JsonNode sourceData = parseJson("{\"system\": {\"value\": 42}}");
        Map<String, JsonNode> sourceDataMap = new HashMap<>();
        sourceDataMap.put("source", sourceData);

        boolean result1 = statefulGraph.evaluate(sourceDataMap);

        assertTrue(result1);
        assertEquals(1, statefulGraph.getSnapshotSize());
    }

    @Test
    @DisplayName("should throw exception when source data is null")
    void testNullSourceData() {
        List<Node> graph = createValidGraph();
        StatefulLogicGraph statefulGraph = new StatefulLogicGraph(graph);

        GraphConstructionException exception = assertThrows(
                GraphConstructionException.class,
                () -> statefulGraph.evaluate(null)
        );

        assertEquals(GraphConstructionException.ErrorType.NULL_INPUT, exception.getErrorType());
        assertTrue(exception.getMessage().contains("Source data cannot be null"));
    }

    @Test
    @DisplayName("should detect changes in provider node values")
    void testChangeDetection() {
        ProviderNode provider = new ProviderNode("source.system.value");
        provider.setId(2);

        ConfigNode configNode = new ConfigNode();
        configNode.setId(1);
        configNode.setInputNodes(Arrays.asList(provider));

        FinalNode finalNode = new FinalNode();
        finalNode.setId(0);
        finalNode.setInputNodes(Arrays.asList(configNode));

        List<Node> graph = Arrays.asList(provider, configNode, finalNode);
        StatefulLogicGraph statefulGraph = new StatefulLogicGraph(graph);

        Map<String, JsonNode> sourceData1 = new HashMap<>();
        sourceData1.put("source", parseJson("{\"system\": {\"value\": 42}}"));

        Map<String, JsonNode> sourceData2 = new HashMap<>();
        sourceData2.put("source", parseJson("{\"system\": {\"value\": 42}}"));

        Map<String, JsonNode> sourceData3 = new HashMap<>();
        sourceData3.put("source", parseJson("{\"system\": {\"value\": 99}}"));

        boolean result1 = statefulGraph.evaluate(sourceData1);
        boolean result2 = statefulGraph.evaluate(sourceData2);
        boolean result3 = statefulGraph.evaluate(sourceData3);

        assertTrue(result1);
        assertFalse(result2);
        assertTrue(result3);
    }

    @Test
    @DisplayName("should return ConfigNode name")
    void testGetConfigNodeName() {
        ConfigNode configNode = new ConfigNode();
        configNode.setId(1);
        configNode.setName("MyConfigNode");

        FinalNode finalNode = new FinalNode();
        finalNode.setId(0);
        finalNode.setInputNodes(Arrays.asList(configNode));

        List<Node> graph = Arrays.asList(configNode, finalNode);
        StatefulLogicGraph statefulGraph = new StatefulLogicGraph(graph);

        String name = statefulGraph.getConfigNodeName();

        assertEquals("MyConfigNode", name);
    }

    @Test
    @DisplayName("should store graph immutably")
    void testGraphImmutability() {
        List<Node> originalGraph = new ArrayList<>(createValidGraph());
        StatefulLogicGraph statefulGraph = new StatefulLogicGraph(originalGraph);

        originalGraph.add(new ConstantNode("Extra", 999));

        assertEquals(2, statefulGraph.getNodeCount());
    }

    // Helper method
    private List<Node> createValidGraph() {
        ConfigNode configNode = new ConfigNode();
        configNode.setId(1);

        FinalNode finalNode = new FinalNode();
        finalNode.setId(0);
        finalNode.setInputNodes(Arrays.asList(configNode));

        return Arrays.asList(configNode, finalNode);
    }
}