package predicates.string_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.string_predicates.StringLengthEqualsOperator;
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
 * Unit tests for the StringLengthEqualsOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: STRING_LENGTH_EQUALS")
public class StringLengthEqualsOperatorTest {

    private StringLengthEqualsOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockStringInput; // Der Input, der den String liefert
    @Mock
    private Node mockLengthInput; // Der Input, der die erwartete Länge als Zahl liefert

    @BeforeEach
    void setUp() {
        operation = new StringLengthEqualsOperator();
    }

    @Test
    @DisplayName("should return true if the string length is equal to the number")
    void testExecute_WhenLengthIsEqual_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockStringInput, mockLengthInput));
        when(mockStringInput.getCalculatedResult()).thenReturn("StayInSync"); // Länge ist 10
        when(mockLengthInput.getCalculatedResult()).thenReturn(10);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if the string length is not equal to the number")
    void testExecute_WhenLengthIsNotEqual_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockStringInput, mockLengthInput));
        when(mockStringInput.getCalculatedResult()).thenReturn("StayInSync"); // Länge ist 10
        when(mockLengthInput.getCalculatedResult()).thenReturn(5);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
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