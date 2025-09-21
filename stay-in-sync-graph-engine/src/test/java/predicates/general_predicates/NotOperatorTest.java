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

import java.util.Collections;
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
    void testExecute_WhenInputIsFalse_ShouldReturnTrue() throws GraphEvaluationException {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(false);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when input is true")
    void testExecute_WhenInputIsTrue_ShouldReturnFalse() throws GraphEvaluationException {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(true);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should throw GraphEvaluationException when input is not a boolean")
    void testExecute_WhenInputIsNotBoolean_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(null); // z.B. null ist kein Boolean

        // ACT & ASSERT
        assertThrows(GraphEvaluationException.class, () -> operation.execute(mockLogicNode, null));
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 1")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(Collections.emptyList()); // 0 Inputs
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode, mockInputNode)); // 2 Inputs
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