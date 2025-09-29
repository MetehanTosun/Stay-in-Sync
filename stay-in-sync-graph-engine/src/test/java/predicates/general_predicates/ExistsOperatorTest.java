package predicates.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.general_predicates.ExistsOperator;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.graphengine.nodes.ProviderNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the ExistsOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: EXISTS")
public class ExistsOperatorTest {

    private ExistsOperator operation;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, Object> dataContext; // CORRECTED: Changed from JsonNode to Object

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private ProviderNode mockInputNode1;
    @Mock
    private ProviderNode mockInputNode2;
    @Mock
    private Node mockInvalidInputNode;

    @BeforeEach
    void setUp() {
        operation = new ExistsOperator();
        // Setup a default data context for happy path tests
        JsonNode sourceNode = objectMapper.createObjectNode()
                .set("sensor", objectMapper.createObjectNode()
                        .put("temperature", 25)
                        .putNull("pressure") // path exists, but value is null
                        .put("humidity", 50));

        // CORRECTED: Put JsonNode directly under "source" key
        dataContext = new HashMap<>();
        dataContext.put("source", sourceNode);
    }

    @Test
    @DisplayName("should return true when the specified path exists")
    void testExecute_WhenPathExists_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.temperature");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true when path exists but its value is null")
    void testExecute_WhenPathExistsWithNullValue_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.pressure");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when the specified path does not exist")
    void testExecute_WhenPathDoesNotExist_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.nonexistent"); // This path is missing

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return true only if ALL specified paths exist")
    void testExecute_WhenMultiplePathsExist_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.temperature");
        when(mockInputNode2.getJsonPath()).thenReturn("source.sensor.humidity");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when one of multiple paths does not exist")
    void testExecute_WhenOneOfMultiplePathsDoesNotExist_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.temperature"); // exists
        when(mockInputNode2.getJsonPath()).thenReturn("source.sensor.nonexistent"); // does not exist

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("should return false when dataContext is null")
    void testExecute_WhenDataContextIsNull_ShouldReturnFalse() {
        // ACT & ASSERT
        assertFalse((Boolean) operation.execute(mockLogicNode, null));
    }

    @Test
    @DisplayName("should return false when dataContext is empty")
    void testExecute_WhenDataContextIsEmpty_ShouldReturnFalse() {
        // ACT & ASSERT
        assertFalse((Boolean) operation.execute(mockLogicNode, Collections.emptyMap()));
    }

    @Test
    @DisplayName("should return false when source key does not exist in dataContext")
    void testExecute_WhenSourceKeyIsMissing_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("missing_source.sensor.temperature");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when source value is null")
    void testExecute_WhenSourceValueIsNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.temperature");

        dataContext.put("source", null); // Null source value

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when source value is not a JsonNode")
    void testExecute_WhenSourceValueIsNotJsonNode_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.temperature");

        dataContext.put("source", "not a json node"); // String instead of JsonNode

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return true when path is just the source key")
    void testExecute_WhenPathIsJustSourceKey_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when path has no parts")
    void testExecute_WhenPathHasNoParts_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn(""); // Empty path

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ==================== NODE VALIDATION TESTS ====================

    @Test
    @DisplayName("should throw exception when inputs list is null")
    void testValidateNode_WhenInputsListIsNull_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(null);

        // ACT & ASSERT
        OperatorValidationException exception = assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });

        assertTrue(exception.getMessage().contains("EXISTS operation requires at least 1 input"));
    }

    @Test
    @DisplayName("should throw exception when inputs list is empty")
    void testValidateNode_WhenInputsListIsEmpty_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(Collections.emptyList());

        // ACT & ASSERT
        OperatorValidationException exception = assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });

        assertTrue(exception.getMessage().contains("EXISTS operation requires at least 1 input"));
    }

    @Test
    @DisplayName("should throw exception when an input is not a ProviderNode")
    void testValidateNode_WhenInputIsNotProviderNode_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInvalidInputNode));

        // ACT & ASSERT
        OperatorValidationException exception = assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });

        assertTrue(exception.getMessage().contains("EXISTS operation requires all inputs to be of type ProviderNode"));
    }

    @Test
    @DisplayName("should pass validation when all inputs are ProviderNodes")
    void testValidateNode_WhenAllInputsAreProviderNodes_ShouldPass() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));

        // ACT & ASSERT
        assertDoesNotThrow(() -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should declare its return type as Boolean")
    void getReturnType_ShouldReturnBooleanClass() {
        assertEquals(Boolean.class, operation.getReturnType());
    }

    // ==================== ADDITIONAL COVERAGE TESTS ====================

    @Test
    @DisplayName("should handle different source keys correctly")
    void testExecute_WithDifferentSourceKeys_ShouldWork() {
        // ARRANGE
        JsonNode otherSourceNode = objectMapper.createObjectNode().put("value", "test");
        dataContext.put("other_source", otherSourceNode);

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("other_source.value");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle deeply nested paths")
    void testExecute_WithDeeplyNestedPath_ShouldWork() {
        // ARRANGE
        JsonNode deepNode = objectMapper.createObjectNode()
                .set("level1", objectMapper.createObjectNode()
                        .set("level2", objectMapper.createObjectNode()
                                .set("level3", objectMapper.createObjectNode()
                                        .put("value", "deep_value"))));
        dataContext.put("source", deepNode);

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.level1.level2.level3.value");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }
}