package predicates.general_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.general_predicates.NotInSetOperator;
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
 * Unit tests for the NotInSetOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: NOT_IN_SET")
public class NotInSetOperatorTest {

    private NotInSetOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockValueInput;
    @Mock
    private Node mockSetInput;

    @BeforeEach
    void setUp() {
        operation = new NotInSetOperator();
    }

    @Test
    @DisplayName("should return true when the value is not present in the list")
    void testExecute_WhenValueIsNotInSet_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueInput, mockSetInput));
        when(mockValueInput.getCalculatedResult()).thenReturn("deleted");
        when(mockSetInput.getCalculatedResult()).thenReturn(List.of("active", "pending", "archived"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when the value is present in the list")
    void testExecute_WhenValueIsInSet_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueInput, mockSetInput));
        when(mockValueInput.getCalculatedResult()).thenReturn("active");
        when(mockSetInput.getCalculatedResult()).thenReturn(List.of("active", "pending", "archived"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 2")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueInput));

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