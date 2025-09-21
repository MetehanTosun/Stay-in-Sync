package predicates.object_predicates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.object_predicates.HasNoKeysOperator;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the HasNoKeysOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: HAS_NO_KEYS")
public class HasNoKeysOperatorTest {

    private HasNoKeysOperator operation;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockObjectInput;
    @Mock
    private Node mockKeysInput;

    @BeforeEach
    void setUp() {
        operation = new HasNoKeysOperator();
    }

    @Test
    @DisplayName("should return true when the object contains none of the specified keys")
    void testExecute_WhenNoKeysExist_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));
        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("status", "OK");
        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(List.of("temperature", "humidity"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when the object contains at least one of the specified keys")
    void testExecute_WhenAtLeastOneKeyExists_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));
        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("temperature", 25);
        jsonObject.put("status", "OK");
        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(List.of("temperature", "humidity"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ==================== INPUT VALIDATION TESTS ====================

    @Test
    @DisplayName("should return true when object input is null")
    void testExecute_WhenObjectInputIsNull_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));
        when(mockObjectInput.getCalculatedResult()).thenReturn(null);
        when(mockKeysInput.getCalculatedResult()).thenReturn(List.of("temperature"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true when object input is not a JsonNode")
    void testExecute_WhenObjectInputIsNotJsonNode_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));
        when(mockObjectInput.getCalculatedResult()).thenReturn("a plain string");
        when(mockKeysInput.getCalculatedResult()).thenReturn(List.of("temperature"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true when keys input is null")
    void testExecute_WhenKeysInputIsNull_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));
        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("temperature", 25);
        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true when keys input is not a Collection")
    void testExecute_WhenKeysInputIsNotACollection_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));
        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("temperature", 25);
        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn("not-a-collection");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true when keys collection is empty")
    void testExecute_WhenKeysCollectionIsEmpty_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));
        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("temperature", 25);
        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(Collections.emptyList());

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should ignore non-string elements in keys collection and return true")
    void testExecute_WhenKeysCollectionHasNonStringsAndNoMatch_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));
        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("status", "OK");
        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(Arrays.asList("temperature", 123, null));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should ignore non-string elements and return false if a string key matches")
    void testExecute_WhenKeysCollectionHasNonStringsAndAMatch_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));
        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("temperature", 25);
        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(Arrays.asList(123, "temperature", null));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ==================== NODE VALIDATION TESTS ====================

    @Test
    @DisplayName("should throw exception if input count is not exactly 2")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput)); // Nur ein Input
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should declare its return type as Boolean")
    void getReturnType_ShouldReturnBooleanClass() {
        assertEquals(Boolean.class, operation.getReturnType());
    }
}