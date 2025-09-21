package predicates.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.general_predicates.NotExistsOperator;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: NOT_EXISTS")
public class NotExistsOperatorTest {

    private NotExistsOperator operation;
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
        operation = new NotExistsOperator();

        // CORRECTED: Simplified setup to create JsonNode directly
        JsonNode sourceNode = objectMapper.createObjectNode()
                .set("sensor", objectMapper.createObjectNode()
                        .put("temperature", 25));

        dataContext = new HashMap<>();
        dataContext.put("source", sourceNode);
    }

    @Test
    @DisplayName("should return true when the specified path does not exist")
    void testExecute_WhenPathDoesNotExist_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.pressure");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when the specified path exists")
    void testExecute_WhenPathExists_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.temperature");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false if any of multiple paths exist")
    void testExecute_WhenOneOfMultiplePathsExists_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.pressure"); // existiert nicht
        when(mockInputNode2.getJsonPath()).thenReturn("source.sensor.temperature"); // existiert

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertFalse((Boolean) result, "Should return false as soon as one path is found.");
    }

    @Test
    @DisplayName("should return true when ALL of multiple paths do not exist")
    void testExecute_WhenAllMultiplePathsDoNotExist_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.pressure"); // existiert nicht
        when(mockInputNode2.getJsonPath()).thenReturn("source.sensor.humidity"); // existiert nicht

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("should return true when dataContext is null")
    void testExecute_WhenDataContextIsNull_ShouldReturnTrue() {
        // ACT & ASSERT
        assertTrue((Boolean) operation.execute(mockLogicNode, null));
    }

    @Test
    @DisplayName("should return true when dataContext is empty")
    void testExecute_WhenDataContextIsEmpty_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.temperature");

        // ACT
        Object result = operation.execute(mockLogicNode, Collections.emptyMap());

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true when source key does not exist in dataContext")
    void testExecute_WhenSourceKeyIsMissing_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("missing_source.sensor.temperature");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true when source value is null")
    void testExecute_WhenSourceValueIsNull_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.temperature");

        dataContext.put("source", null); // Null source value

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true when source value is not a JsonNode")
    void testExecute_WhenSourceValueIsNotJsonNode_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.temperature");

        dataContext.put("source", "not a json node"); // String instead of JsonNode

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true when path has no parts")
    void testExecute_WhenPathHasNoParts_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn(""); // Empty path

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle non-ProviderNode inputs gracefully")
    void testExecute_WhenInputIsNotProviderNode_ShouldSkip() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInvalidInputNode));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.nonexistent"); // doesn't exist

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result); // Should skip non-ProviderNode and return true since no paths exist
    }

    // ==================== NODE VALIDATION TESTS ====================

    @Test
    @DisplayName("should throw exception when inputs list is null")
    void testValidateNode_WhenInputsListIsNull_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(null);
        when(mockLogicNode.getName()).thenReturn("TestNode");

        // ACT & ASSERT
        OperatorValidationException exception = assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });

        assertTrue(exception.getMessage().contains("NOT_EXISTS operation for node 'TestNode' requires at least 1 input"));
    }

    @Test
    @DisplayName("should throw exception when inputs list is empty")
    void testValidateNode_WhenInputsListIsEmpty_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(Collections.emptyList());
        when(mockLogicNode.getName()).thenReturn("TestNode");

        // ACT & ASSERT
        OperatorValidationException exception = assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });

        assertTrue(exception.getMessage().contains("NOT_EXISTS operation for node 'TestNode' requires at least 1 input"));
    }

    @Test
    @DisplayName("should throw exception when an input is not a ProviderNode")
    void testValidateNode_WhenInputIsNotProviderNode_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInvalidInputNode));
        when(mockLogicNode.getName()).thenReturn("TestNode");

        // ACT & ASSERT
        OperatorValidationException exception = assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });

        assertTrue(exception.getMessage().contains("NOT_EXISTS operation for node 'TestNode' requires all its inputs to be of type ProviderNode"));
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
        JsonNode otherSourceNode = objectMapper.createObjectNode().put("nonexistent", "test");
        dataContext.put("other_source", otherSourceNode);

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("other_source.missing_path");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result); // Path doesn't exist, so NOT_EXISTS returns true
    }

    @Test
    @DisplayName("should return false when path is just the source key and source exists")
    void testExecute_WhenPathIsJustSourceKeyAndExists_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertFalse((Boolean) result); // Source exists, so NOT_EXISTS returns false
    }

    @Test
    @DisplayName("should return true when checking multiple non-existing paths")
    void testExecute_WithMultipleNonExistingPaths_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.pressure");
        when(mockInputNode2.getJsonPath()).thenReturn("source.sensor.altitude");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result); // None of the paths exist
    }
}