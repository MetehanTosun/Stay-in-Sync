package predicates.boolean_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.boolean_predicates.IsFalseOperator;
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
 * Unit tests for the IsFalseOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: IS_FALSE")
public class IsFalseOperatorTest {

    private IsFalseOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockInputNode;

    @BeforeEach
    void setUp() {
        operation = new IsFalseOperator();
    }

    @Test
    @DisplayName("should return true when input is Boolean.FALSE")
    void testExecute_WhenInputIsFalse_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(Boolean.FALSE);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when input is Boolean.TRUE")
    void testExecute_WhenInputIsTrue_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(Boolean.TRUE);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when input is not a Boolean")
    void testExecute_WhenInputIsNotBoolean_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn("false"); // A String, not a Boolean

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should throw exception if there are no inputs")
    void testValidateNode_WithNoInputs_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of()); // Empty list

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