package predicates.object_predicates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.object_predicates.HasAnyKeyOperator;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the HasAnyKeyOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: HAS_ANY_KEY")
public class HasAnyKeyOperatorTest {

    private HasAnyKeyOperator operation;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockObjectInput; // The input that provides the JSON object
    @Mock
    private Node mockKeysInput;   // The input that provides the key name

    @BeforeEach
    void setUp() {
        operation = new HasAnyKeyOperator();
    }

    @Test
    @DisplayName("should return true when the object contains at least one of the specified keys")
    void testExecute_WhenAtLeastOneKeyExists_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("status", "OK");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        // Das Objekt hat "status", aber nicht "temperature" oder "humidity"
        when(mockKeysInput.getCalculatedResult()).thenReturn(List.of("temperature", "humidity", "status"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when the object contains none of the specified keys")
    void testExecute_WhenNoKeysExist_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("unrelated_key", "some_value");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(List.of("temperature", "humidity", "status"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 2")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput));

        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });
    }

    @Test
    @DisplayName("should declare its return type as Boolean")
    void getReturnType_ShouldReturnBooleanClass() {
        assertEquals(Boolean.class, operation.getReturnType());
    }

    @Test
    @DisplayName("should return false when object input is null")
    void testExecute_WhenObjectInputIsNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));
        when(mockObjectInput.getCalculatedResult()).thenReturn(null);
        when(mockKeysInput.getCalculatedResult()).thenReturn(List.of("key1", "key2"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when keys input is null")
    void testExecute_WhenKeysInputIsNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("key1", "value1");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ==================== NON-JSONNODE INPUT TESTS ====================

    @Test
    @DisplayName("should return false when object input is not a JsonNode")
    void testExecute_WhenObjectInputIsNotJsonNode_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));
        when(mockObjectInput.getCalculatedResult()).thenReturn("not a json node");
        when(mockKeysInput.getCalculatedResult()).thenReturn(List.of("key1"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when JsonNode is not an object")
    void testExecute_WhenJsonNodeIsNotObject_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ArrayNode jsonArray = objectMapper.createArrayNode();
        jsonArray.add("value1");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonArray);
        when(mockKeysInput.getCalculatedResult()).thenReturn(List.of("key1"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ==================== NON-COLLECTION KEYS TESTS ====================

    @Test
    @DisplayName("should return false when keys input is not a Collection")
    void testExecute_WhenKeysInputIsNotCollection_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("key1", "value1");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn("not a collection");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ==================== COLLECTION WITH NON-STRING KEYS ====================

    @Test
    @DisplayName("should skip non-string keys and continue checking")
    void testExecute_WhenCollectionContainsNonStringKeys_ShouldSkipAndContinue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("key2", "value2");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(List.of(123, "key2", Boolean.TRUE));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Should find "key2" and skip non-string keys
    }

    @Test
    @DisplayName("should return false when all keys in collection are non-strings")
    void testExecute_WhenAllKeysAreNonStrings_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("key1", "value1");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(List.of(123, Boolean.TRUE, 45.6));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ==================== EMPTY COLLECTION TESTS ====================

    @Test
    @DisplayName("should return false when keys collection is empty")
    void testExecute_WhenKeysCollectionIsEmpty_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("key1", "value1");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(Collections.emptyList());

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ==================== DIFFERENT COLLECTION TYPES ====================

    @Test
    @DisplayName("should work with Set as keys collection")
    void testExecute_WithSetAsKeysCollection_ShouldWork() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("key2", "value2");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(Set.of("key1", "key2", "key3"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @DisplayName("should throw exception when inputs list is null")
    void testValidateNode_WhenInputsListIsNull_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(null);

        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });
    }

    @Test
    @DisplayName("should throw exception when there are too many inputs")
    void testValidateNode_WhenTooManyInputs_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput, mockObjectInput));

        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("should handle collection with null elements")
    void testExecute_WithCollectionContainingNullElements_ShouldSkipNulls() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("key2", "value2");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);

        // KORREKTUR: Ersetze List.of() durch eine Methode, die null-Werte erlaubt.
        when(mockKeysInput.getCalculatedResult()).thenReturn(java.util.Arrays.asList(null, "key2", null));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Should find "key2" and skip nulls
    }
}