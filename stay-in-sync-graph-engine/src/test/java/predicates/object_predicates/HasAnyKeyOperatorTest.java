package predicates.object_predicates;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;

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
}