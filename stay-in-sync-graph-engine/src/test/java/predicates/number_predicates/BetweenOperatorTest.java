package predicates.number_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.number_predicates.BetweenOperator;
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
 * Unit tests for the BetweenOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: BETWEEN")
public class BetweenOperatorTest {

    private BetweenOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockValueNode; // Der zu prüfende Wert
    @Mock
    private Node mockLowerBoundNode; // Die untere Grenze
    @Mock
    private Node mockUpperBoundNode; // Die obere Grenze

    @BeforeEach
    void setUp() {
        operation = new BetweenOperator();
    }

    @Test
    @DisplayName("should return true if value is within the bounds")
    void testExecute_WhenValueIsBetween_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueNode, mockLowerBoundNode, mockUpperBoundNode));
        when(mockValueNode.getCalculatedResult()).thenReturn(15.0);
        when(mockLowerBoundNode.getCalculatedResult()).thenReturn(10.0);
        when(mockUpperBoundNode.getCalculatedResult()).thenReturn(20.0);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true if value is equal to a bound (inclusive)")
    void testExecute_WhenValueIsOnBound_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueNode, mockLowerBoundNode, mockUpperBoundNode));
        when(mockLowerBoundNode.getCalculatedResult()).thenReturn(10.0);
        when(mockUpperBoundNode.getCalculatedResult()).thenReturn(20.0);

        // ACT 1: Equal to lower bound
        when(mockValueNode.getCalculatedResult()).thenReturn(10.0);
        Object result1 = operation.execute(mockLogicNode, null);

        // ACT 2: Equal to upper bound
        when(mockValueNode.getCalculatedResult()).thenReturn(20.0);
        Object result2 = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result1, "Should be true when value equals lower bound.");
        assertTrue((Boolean) result2, "Should be true when value equals upper bound.");
    }

    @Test
    @DisplayName("should return false if value is outside the bounds")
    void testExecute_WhenValueIsOutside_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueNode, mockLowerBoundNode, mockUpperBoundNode));
        when(mockLowerBoundNode.getCalculatedResult()).thenReturn(10.0);
        when(mockUpperBoundNode.getCalculatedResult()).thenReturn(20.0);

        // ACT 1: Below lower bound
        when(mockValueNode.getCalculatedResult()).thenReturn(5.0);
        Object result1 = operation.execute(mockLogicNode, null);

        // ACT 2: Above upper bound
        when(mockValueNode.getCalculatedResult()).thenReturn(25.0);
        Object result2 = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result1, "Should be false when value is below lower bound.");
        assertFalse((Boolean) result2, "Should be false when value is above upper bound.");
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 3")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueNode, mockLowerBoundNode)); // Only two inputs

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