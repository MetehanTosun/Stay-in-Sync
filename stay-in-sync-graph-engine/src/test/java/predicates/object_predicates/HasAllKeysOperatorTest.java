package predicates.object_predicates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.object_predicates.HasAllKeysOperator;
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
 * Unit tests for the HasAllKeysOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: HAS_ALL_KEYS")
public class HasAllKeysOperatorTest {

    private HasAllKeysOperator operation;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockObjectInput; // The input that provides the JSON object
    @Mock
    private Node mockKeysInput;   // The input that provides the key name

    @BeforeEach
    void setUp() {
        operation = new HasAllKeysOperator();
    }

    @Test
    @DisplayName("should return true when the object contains all specified keys")
    void testExecute_WhenAllKeysExist_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("temperature", 25);
        jsonObject.put("humidity", 50);
        jsonObject.put("status", "OK");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(List.of("temperature", "humidity"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when the object is missing one of the specified keys")
    void testExecute_WhenOneKeyIsMissing_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("temperature", 25);
        jsonObject.put("status", "OK");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        // "humidity" fehlt im Objekt
        when(mockKeysInput.getCalculatedResult()).thenReturn(List.of("temperature", "humidity"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 2")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput)); // Nur ein Input

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

    // ==================== EMPTY COLLECTION TESTS ====================

    @Test
    @DisplayName("should return true when keys collection is empty")
    void testExecute_WhenKeysCollectionIsEmpty_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("key1", "value1");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(Collections.emptyList());

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Vacuously true
    }

    // ==================== NON-STRING KEYS TESTS ====================

    @Test
    @DisplayName("should return false when collection contains non-string keys")
    void testExecute_WhenCollectionContainsNonStringKeys_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("key1", "value1");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(List.of("key1", 123, "key2"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when collection contains null keys")
    void testExecute_WhenCollectionContainsNullKeys_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("key1", "value1");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);

        // KORREKTUR: Verwende eine ArrayList, um eine Liste mit 'null' zu erstellen.
        List<String> keysWithNull = new java.util.ArrayList<>();
        keysWithNull.add("key1");
        keysWithNull.add(null);
        when(mockKeysInput.getCalculatedResult()).thenReturn(keysWithNull);

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
        jsonObject.put("key1", "value1");
        jsonObject.put("key2", "value2");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(Set.of("key1", "key2"));

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
    @DisplayName("should handle JsonNode with null values")
    void testExecute_WithJsonNodeWithNullValues_ShouldWork() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.putNull("key1");
        jsonObject.put("key2", "value2");

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(List.of("key1", "key2"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Keys exist, even if value is null
    }

    @Test
    @DisplayName("should return false when one key is missing from object")
    void testExecute_WhenOneKeyIsMissingFromObject_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeysInput));

        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("key1", "value1");
        // key2 is missing

        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeysInput.getCalculatedResult()).thenReturn(List.of("key1", "key2"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }
}