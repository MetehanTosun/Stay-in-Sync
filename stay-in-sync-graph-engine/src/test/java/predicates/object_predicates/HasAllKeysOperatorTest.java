package predicates.object_predicates;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;

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
}