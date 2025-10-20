package predicates.general_predicates;

import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.general_predicates.XorOperator;
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
 * Unit tests for the XorOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: XOR")
public class XorOperatorTest {

    private XorOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockInputNode1;
    @Mock
    private Node mockInputNode2;
    @Mock
    private Node mockInputNode3;

    @BeforeEach
    void setUp() {
        operation = new XorOperator();
    }

    @Test
    @DisplayName("should return true when exactly one input is true")
    void testExecute_WhenOneInputIsTrue_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(true);
        when(mockInputNode2.getCalculatedResult()).thenReturn(false);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when both inputs are the same")
    void testExecute_WhenBothInputsAreSame_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));

        // Fall 1: Beide sind true
        when(mockInputNode1.getCalculatedResult()).thenReturn(true);
        when(mockInputNode2.getCalculatedResult()).thenReturn(true);
        Object result1 = operation.execute(mockLogicNode, null);

        // Fall 2: Beide sind false
        when(mockInputNode1.getCalculatedResult()).thenReturn(false);
        when(mockInputNode2.getCalculatedResult()).thenReturn(false);
        Object result2 = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result1, "Should be false when both are true.");
        assertFalse((Boolean) result2, "Should be false when both are false.");
    }

    @Test
    @DisplayName("should throw GraphEvaluationException when input is not a boolean")
    void testExecute_WhenInputIsNotBoolean_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn("true");

        // ACT & ASSERT
        assertThrows(GraphEvaluationException.class, () -> operation.execute(mockLogicNode, null));
    }

    @Test
    @DisplayName("should ignore null inputs")
    void testExecute_WhenInputIsNull_ShouldBeIgnored() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));
        when(mockInputNode1.getCalculatedResult()).thenReturn(true);
        when(mockInputNode2.getCalculatedResult()).thenReturn(false);
        when(mockInputNode3.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 2")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));

        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should declare its return type as Boolean")
    void getReturnType_ShouldReturnBooleanClass() {
        assertEquals(Boolean.class, operation.getReturnType());
    }
}