package predicates.general_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.general_predicates.TypeIsOperator;
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
 * Unit tests for the TypeIsOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: TYPE_IS")
public class TypeIsOperatorTest {

    private TypeIsOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockTypeInput;
    @Mock
    private Node mockValueInput;

    @BeforeEach
    void setUp() {
        operation = new TypeIsOperator();
    }

    @Test
    @DisplayName("should return true when the value's type matches the string")
    void testExecute_WhenTypeMatches_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockTypeInput, mockValueInput));
        when(mockTypeInput.getCalculatedResult()).thenReturn("NUMBER");
        when(mockValueInput.getCalculatedResult()).thenReturn(123.45);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when the value's type does not match the string")
    void testExecute_WhenTypeDoesNotMatch_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockTypeInput, mockValueInput));
        when(mockTypeInput.getCalculatedResult()).thenReturn("BOOLEAN");
        when(mockValueInput.getCalculatedResult()).thenReturn("true");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should throw exception if input count is less than 2")
    void testValidateNode_WithLessThanTwoInputs_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockTypeInput));

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