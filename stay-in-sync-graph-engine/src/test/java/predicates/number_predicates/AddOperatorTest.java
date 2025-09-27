package predicates.number_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.number_predicates.AddOperator;
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
 * Unit tests for the AddOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: ADD")
public class AddOperatorTest {

    private AddOperator operation;

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
        operation = new AddOperator();
    }

    @Test
    @DisplayName("should return the correct sum of multiple number inputs")
    void testExecute_WithMultipleNumbers_ShouldReturnSum() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));
        when(mockInputNode1.getCalculatedResult()).thenReturn(10.5); // Double
        when(mockInputNode2.getCalculatedResult()).thenReturn(5);    // Integer
        when(mockInputNode3.getCalculatedResult()).thenReturn(2.5);  // Double

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertEquals(18.0, (Double) result);
    }

    @Test
    @DisplayName("should throw exception if any input is not a number")
    void testExecute_WithNonNumericInput_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(10.0);
        when(mockInputNode2.getCalculatedResult()).thenReturn("not a number"); // Invalid type

        // ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> {
            operation.execute(mockLogicNode, null);
        });
    }

    @Test
    @DisplayName("should throw exception if input count is less than 2")
    void testValidateNode_WithLessThanTwoInputs_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1)); // Only one input

        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });
    }

    @Test
    @DisplayName("should declare its return type as Double")
    void getReturnType_ShouldReturnDoubleClass() {
        assertEquals(Double.class, operation.getReturnType());
    }
}