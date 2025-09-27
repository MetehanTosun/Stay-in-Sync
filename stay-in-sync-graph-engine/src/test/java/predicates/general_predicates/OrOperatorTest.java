package predicates.general_predicates;

import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.general_predicates.OrOperator;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the OrOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: OR")
public class OrOperatorTest {

    private OrOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockInputNode1;
    @Mock
    private Node mockInputNode2;

    @BeforeEach
    void setUp() {
        operation = new OrOperator();
    }

    @Test
    @DisplayName("should return true when any input is true")
    void testExecute_WhenAnyInputIsTrue_ShouldReturnTrue() throws GraphEvaluationException {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(false);
        when(mockInputNode2.getCalculatedResult()).thenReturn(true);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when all inputs are false")
    void testExecute_WhenAllInputsAreFalse_ShouldReturnFalse() throws GraphEvaluationException {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(false);
        when(mockInputNode2.getCalculatedResult()).thenReturn(false);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should throw GraphEvaluationException when input is not a boolean")
    void testExecute_WhenInputIsNotBoolean_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(123); // Falscher Typ

        // ACT & ASSERT
        assertThrows(GraphEvaluationException.class, () -> operation.execute(mockLogicNode, null));
    }

    @Test
    @DisplayName("should throw exception if input count is less than 2")
    void testValidateNode_WithLessThanTwoInputs_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));

        when(mockLogicNode.getInputNodes()).thenReturn(null);
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should declare its return type as Boolean")
    void getReturnType_ShouldReturnBooleanClass() {
        assertEquals(Boolean.class, operation.getReturnType());
    }
}