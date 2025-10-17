package nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.exception.NodeConfigurationException;
import de.unistuttgart.graphengine.nodes.ProviderNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProviderNode Tests")
public class ProviderNodeTest {

    private Map<String, Object> dataContext;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        dataContext = new HashMap<>();
    }

    // ===== CONSTRUCTOR VALIDATION TESTS =====

    @Test
    @DisplayName("should create ProviderNode with valid jsonPath")
    void testConstructor_WithValidJsonPath_ShouldSucceed()  {
        // ARRANGE & ACT
        ProviderNode node = new ProviderNode("source.system1.value");

        // ASSERT
        assertEquals("source.system1.value", node.getJsonPath());
    }

    @Test
    @DisplayName("should throw exception for null jsonPath")
    void testConstructor_WithNullJsonPath_ShouldThrowException() {
        // ACT & ASSERT
        NodeConfigurationException exception = assertThrows(NodeConfigurationException.class, () -> {
            new ProviderNode(null);
        });

        assertTrue(exception.getMessage().contains("jsonPath for ProviderNode cannot be null or empty"));
    }

    @Test
    @DisplayName("should throw exception for empty jsonPath")
    void testConstructor_WithEmptyJsonPath_ShouldThrowException() {
        // ACT & ASSERT
        NodeConfigurationException exception = assertThrows(NodeConfigurationException.class, () -> {
            new ProviderNode("");
        });

        assertTrue(exception.getMessage().contains("jsonPath for ProviderNode cannot be null or empty"));
    }

    @Test
    @DisplayName("should throw exception for whitespace-only jsonPath")
    void testConstructor_WithWhitespaceJsonPath_ShouldThrowException() {
        // ACT & ASSERT
        NodeConfigurationException exception = assertThrows(NodeConfigurationException.class, () -> {
            new ProviderNode("   \t\n   ");
        });

        assertTrue(exception.getMessage().contains("jsonPath for ProviderNode cannot be null or empty"));
    }

    @Test
    @DisplayName("should throw exception when jsonPath does not start with 'source'")
    void testConstructor_WithoutSourcePrefix_ShouldThrowException() {
        // ACT & ASSERT
        NodeConfigurationException exception = assertThrows(NodeConfigurationException.class, () -> {
            new ProviderNode("data.system1.value");
        });

        assertTrue(exception.getMessage().contains("jsonPath for ProviderNode must start with 'source'"));
    }

    @Test
    @DisplayName("should throw exception for invalid jsonPath format")
    void testConstructor_WithInvalidFormat_ShouldThrowException() {
        // ACT & ASSERT
        NodeConfigurationException exception = assertThrows(NodeConfigurationException.class, () -> {
            new ProviderNode("source"); // Missing sourceName
        });

        assertTrue(exception.getMessage().contains("Invalid jsonPath format"));
        assertTrue(exception.getMessage().contains("Must contain 'source.{sourceName}'"));
    }

    // ===== CALCULATE METHOD TESTS =====

    @Test
    @DisplayName("should return Object.class as output type")
    void testGetOutputType() throws Exception {
        ProviderNode node = new ProviderNode("source.system1.value");
        assertEquals(Object.class, node.getOutputType());
    }

    @Test
    @DisplayName("should throw exception when 'source' key missing from dataContext")
    void testCalculate_WithoutSourceKey_ShouldThrowException() {
        // ARRANGE
        ProviderNode node = new ProviderNode("source.system1.value");
        // dataContext without 'source' key

        // ACT & ASSERT
        GraphEvaluationException exception = assertThrows(GraphEvaluationException.class, () -> {
            node.calculate(dataContext);
        });

        assertEquals(GraphEvaluationException.ErrorType.DATA_NOT_FOUND, exception.getErrorType());
        // CORRECTED: Updated to match new exception message
        assertTrue(exception.getMessage().contains("The dataContext must contain a non-null entry for the key 'source'"));
    }

    @Test
    @DisplayName("should throw exception when source value is null")
    void testCalculate_WithNullSourceValue_ShouldThrowException() {
        // ARRANGE
        ProviderNode node = new ProviderNode("source.system1.value");
        dataContext.put("source", null);

        // ACT & ASSERT
        GraphEvaluationException exception = assertThrows(GraphEvaluationException.class, () -> {
            node.calculate(dataContext);
        });

        assertEquals(GraphEvaluationException.ErrorType.DATA_NOT_FOUND, exception.getErrorType());
        // CORRECTED: Updated to match new exception message
        assertTrue(exception.getMessage().contains("The dataContext must contain a non-null entry for the key 'source'"));
    }

    @Test
    @DisplayName("should throw exception when source value is not a JsonNode")
    void testCalculate_WithNonJsonNodeSource_ShouldThrowException() {
        // ARRANGE
        ProviderNode node = new ProviderNode("source.system1.value");
        dataContext.put("source", "not a json node"); // String instead of JsonNode

        // ACT & ASSERT
        GraphEvaluationException exception = assertThrows(GraphEvaluationException.class, () -> {
            node.calculate(dataContext);
        });

        assertEquals(GraphEvaluationException.ErrorType.TYPE_MISMATCH, exception.getErrorType());
        assertTrue(exception.getMessage().contains("The value for 'source' in dataContext must be a JsonNode"));
        assertTrue(exception.getMessage().contains("java.lang.String"));
    }

    @Test
    @DisplayName("should extract value successfully from valid path")
    void testCalculate_WithValidPath_ShouldExtractValue() {
        // ARRANGE
        ProviderNode node = new ProviderNode("source.system1.sensors.temperature");

        // Create nested JSON structure
        Map<String, Object> sourceData = Map.of(
                "system1", Map.of(
                        "sensors", Map.of(
                                "temperature", 25.5
                        )
                )
        );
        JsonNode sourceNode = objectMapper.valueToTree(sourceData);
        dataContext.put("source", sourceNode);

        // ACT
        node.calculate(dataContext);

        // ASSERT
        assertEquals(25.5, node.getCalculatedResult());
    }

    @Test
    @DisplayName("should set null when path does not exist")
    void testCalculate_WithNonExistentPath_ShouldSetNull(){
        // ARRANGE
        ProviderNode node = new ProviderNode("source.system1.nonexistent.path");

        Map<String, Object> sourceData = Map.of("system1", Map.of("other", "value"));
        JsonNode sourceNode = objectMapper.valueToTree(sourceData);
        dataContext.put("source", sourceNode);

        // ACT
        node.calculate(dataContext);

        // ASSERT
        assertNull(node.getCalculatedResult());
    }

    @Test
    @DisplayName("should handle complex jsonPath correctly")
    void testCalculate_WithComplexPath_ShouldWork() {
        // ARRANGE
        ProviderNode node = new ProviderNode("source.complex.nested.deep.structure.value");

        Map<String, Object> sourceData = Map.of(
                "complex", Map.of(
                        "nested", Map.of(
                                "deep", Map.of(
                                        "structure", Map.of(
                                                "value", "found_it"
                                        )
                                )
                        )
                )
        );
        JsonNode sourceNode = objectMapper.valueToTree(sourceData);
        dataContext.put("source", sourceNode);

        // ACT
        node.calculate(dataContext);

        // ASSERT
        assertEquals("found_it", node.getCalculatedResult());
    }

    // ===== ADDITIONAL COVERAGE TESTS =====

    @Test
    @DisplayName("should handle empty object in source")
    void testCalculate_WithEmptySource_ShouldSetNull() {
        // ARRANGE
        ProviderNode node = new ProviderNode("source.system1.value");

        JsonNode sourceNode = objectMapper.createObjectNode(); // Empty JSON object
        dataContext.put("source", sourceNode);

        // ACT
        node.calculate(dataContext);

        // ASSERT
        assertNull(node.getCalculatedResult());
    }

    @Test
    @DisplayName("should handle different value types correctly")
    void testCalculate_WithDifferentValueTypes_ShouldWork(){
        // Test String value
        ProviderNode stringNode = new ProviderNode("source.data.stringValue");
        Map<String, Object> stringData = Map.of("data", Map.of("stringValue", "test"));
        JsonNode stringSource = objectMapper.valueToTree(stringData);
        dataContext.put("source", stringSource);
        stringNode.calculate(dataContext);
        assertEquals("test", stringNode.getCalculatedResult());

        // Test Integer value
        ProviderNode intNode = new ProviderNode("source.data.intValue");
        Map<String, Object> intData = Map.of("data", Map.of("intValue", 42));
        JsonNode intSource = objectMapper.valueToTree(intData);
        dataContext.put("source", intSource);
        intNode.calculate(dataContext);
        assertEquals(42, intNode.getCalculatedResult());

        // Test Boolean value
        ProviderNode boolNode = new ProviderNode("source.data.boolValue");
        Map<String, Object> boolData = Map.of("data", Map.of("boolValue", true));
        JsonNode boolSource = objectMapper.valueToTree(boolData);
        dataContext.put("source", boolSource);
        boolNode.calculate(dataContext);
        assertEquals(true, boolNode.getCalculatedResult());
    }

    @Test
    @DisplayName("should handle minimal valid jsonPath")
    void testConstructor_WithMinimalValidPath_ShouldSucceed() {
        // ARRANGE & ACT
        ProviderNode node = new ProviderNode("source.sys");

        // ASSERT
        assertEquals("source.sys", node.getJsonPath());
    }

    @Test
    @DisplayName("should handle arcId getter and setter")
    void testArcIdHandling() {
        // ARRANGE
        ProviderNode node = new ProviderNode("source.system1.value");

        // ACT & ASSERT
        assertNull(node.getArcId()); // Default should be null

        node.setArcId(123);
        assertEquals(Integer.valueOf(123), node.getArcId());

        node.setArcId(null);
        assertNull(node.getArcId());
    }
}