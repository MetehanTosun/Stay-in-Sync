package predicates.datetime_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.datetime_predicates.AfterOperator;
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
 * Unit tests for the AfterOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: AFTER")
public class AfterOperatorTest {

    private AfterOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockInputNode1;
    @Mock
    private Node mockInputNode2;

    @BeforeEach
    void setUp() {
        operation = new AfterOperator();
    }

    @Test
    @DisplayName("should return true if the first date is after the second")
    void testExecute_WhenFirstDateIsAfter_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn("2025-09-15T14:00:00Z");
        when(mockInputNode2.getCalculatedResult()).thenReturn("2025-09-15T12:00:00Z");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if the first date is not after the second")
    void testExecute_WhenFirstDateIsNotAfter_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));

        // Case 1: Date is before
        when(mockInputNode1.getCalculatedResult()).thenReturn("2025-09-15T10:00:00Z");
        when(mockInputNode2.getCalculatedResult()).thenReturn("2025-09-15T12:00:00Z");
        Object resultBefore = operation.execute(mockLogicNode, null);

        // Case 2: Date is the same
        when(mockInputNode1.getCalculatedResult()).thenReturn("2025-09-15T12:00:00Z");
        Object resultSame = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) resultBefore, "Should be false when date is before.");
        assertFalse((Boolean) resultSame, "Should be false when dates are the same.");
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 2")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));

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