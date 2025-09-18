package predicates.general_predicates;

import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.general_predicates.NotOperator;
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
 * Unit tests for the NotOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: NOT")
public class NotOperatorTest {

    private NotOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockInputNode;

    @BeforeEach
    void setUp() {
        operation = new NotOperator();
    }

    @Test
    @DisplayName("should return true when input is false")
    void testExecute_WhenInputIsFalse_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(false);

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertTrue((Boolean) result);
        });
    }

    @Test
    @DisplayName("should return false when input is true")
    void testExecute_WhenInputIsTrue_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(true);

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertFalse((Boolean) result);
        });
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 1")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        // Fall 1: Keine Inputs
        when(mockLogicNode.getInputNodes()).thenReturn(List.of());
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));

        // Fall 2: Mehr als ein Input
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode, mockInputNode));
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should declare its return type as Boolean")
    void getReturnType_ShouldReturnBooleanClass() {
        assertEquals(Boolean.class, operation.getReturnType());
    }
}