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

    private ProviderNode providerNode;
    private Map<String, JsonNode> dataContext;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        dataContext = new HashMap<>();
    }

    // ===== CONSTRUCTOR VALIDATION TESTS =====

    @Test
    @DisplayName("should create ProviderNode with valid jsonPath")
    void testConstructor_WithValidJsonPath_ShouldSucceed() throws Exception {
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
    void testCalculate_WithoutSourceKey_ShouldThrowException() throws Exception {
        // ARRANGE
        ProviderNode node = new ProviderNode("source.system1.value");
        // dataContext without 'source' key

        // ACT & ASSERT
        GraphEvaluationException exception = assertThrows(GraphEvaluationException.class, () -> {
            node.calculate(dataContext);
        });

        assertEquals(GraphEvaluationException.ErrorType.DATA_NOT_FOUND, exception.getErrorType());
        assertTrue(exception.getMessage().contains("'source' is not the first scoped key"));
    }

    @Test
    @DisplayName("should throw exception when source value is null")
    void testCalculate_WithNullSourceValue_ShouldThrowException() throws Exception {
        // ARRANGE
        ProviderNode node = new ProviderNode("source.system1.value");
        dataContext.put("source", null);

        // ACT & ASSERT
        GraphEvaluationException exception = assertThrows(GraphEvaluationException.class, () -> {
            node.calculate(dataContext);
        });

        assertEquals(GraphEvaluationException.ErrorType.DATA_NOT_FOUND, exception.getErrorType());
        assertTrue(exception.getMessage().contains("no defined sourceSystemNames found under 'source'"));
    }

    @Test
    @DisplayName("should extract value successfully from valid path")
    void testCalculate_WithValidPath_ShouldExtractValue() throws Exception {
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
    void testCalculate_WithNonExistentPath_ShouldSetNull() throws Exception {
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
    void testCalculate_WithComplexPath_ShouldWork() throws Exception {
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
}
