package predicates.object_predicates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.object_predicates.LacksKeyOperator;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the LacksKeyOperation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: LACKS_KEY")
public class LacksKeyOperationTest {

    private LacksKeyOperator operation;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockObjectInput; // The input that provides the JSON object
    @Mock
    private Node mockKeyInput;    // The input that provides the key name

    @BeforeEach
    void setUp() {
        operation = new LacksKeyOperator();
    }

    @Test
    @DisplayName("should return true when the object does not have the key")
    void testExecute_WhenKeyDoesNotExist_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeyInput));
        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("humidity", 50);
        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeyInput.getCalculatedResult()).thenReturn("temperature");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when the object has the key")
    void testExecute_WhenKeyExists_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeyInput));
        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("temperature", 25);
        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeyInput.getCalculatedResult()).thenReturn("temperature");

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
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeyInput));
        when(mockObjectInput.getCalculatedResult()).thenReturn(null);
        when(mockKeyInput.getCalculatedResult()).thenReturn("temperature");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true when object input is not a JsonNode")
    void testExecute_WhenObjectInputIsNotJsonNode_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeyInput));
        when(mockObjectInput.getCalculatedResult()).thenReturn("a plain string");
        when(mockKeyInput.getCalculatedResult()).thenReturn("temperature");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true when JsonNode is not an object (e.g., an array)")
    void testExecute_WhenJsonNodeIsNotAnObject_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeyInput));
        ArrayNode jsonArray = objectMapper.createArrayNode();
        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonArray);
        when(mockKeyInput.getCalculatedResult()).thenReturn("temperature");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when key input is null")
    void testExecute_WhenKeyInputIsNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeyInput));
        ObjectNode jsonObject = objectMapper.createObjectNode();
        jsonObject.put("temperature", 25);
        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeyInput.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when key input is not a String")
    void testExecute_WhenKeyInputIsNotAString_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput, mockKeyInput));
        ObjectNode jsonObject = objectMapper.createObjectNode();
        when(mockObjectInput.getCalculatedResult()).thenReturn(jsonObject);
        when(mockKeyInput.getCalculatedResult()).thenReturn(123); // Integer statt String

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
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockObjectInput));

        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should declare its return type as Boolean")
    void getReturnType_ShouldReturnBooleanClass() {
        assertEquals(Boolean.class, operation.getReturnType());
    }
}