package predicates.string_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.string_predicates.StringLengthGtOperator;
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
 * Unit tests for the StringLengthGtOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: STRING_LENGTH_GT")
public class StringLengthGtOperatorTest {

    private StringLengthGtOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockStringInput;
    @Mock
    private Node mockLengthInput;

    @BeforeEach
    void setUp() {
        operation = new StringLengthGtOperator();
    }

    @Test
    @DisplayName("should return true if the string length is greater than the number")
    void testExecute_WhenLengthIsGreater_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockStringInput, mockLengthInput));
        when(mockStringInput.getCalculatedResult()).thenReturn("StayInSync"); // Länge ist 10
        when(mockLengthInput.getCalculatedResult()).thenReturn(9);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if the string length is not greater than the number")
    void testExecute_WhenLengthIsNotGreater_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockStringInput, mockLengthInput));
        when(mockStringInput.getCalculatedResult()).thenReturn("StayInSync"); // Länge ist 10

        // Fall 1: Länge ist kleiner
        when(mockLengthInput.getCalculatedResult()).thenReturn(11);
        Object result1 = operation.execute(mockLogicNode, null);

        // Fall 2: Länge ist gleich
        when(mockLengthInput.getCalculatedResult()).thenReturn(10);
        Object result2 = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result1, "Should be false when length is less.");
        assertFalse((Boolean) result2, "Should be false when length is equal.");
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 2")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockStringInput));

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