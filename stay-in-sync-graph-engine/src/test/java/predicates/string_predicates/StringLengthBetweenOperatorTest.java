package predicates.string_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.string_predicates.StringLengthBetweenOperator;
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
 * Unit tests for the StringLengthBetweenOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: STRING_LENGTH_BETWEEN")
public class StringLengthBetweenOperatorTest {

    private StringLengthBetweenOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockStringInput;
    @Mock
    private Node mockMinLengthInput;
    @Mock
    private Node mockMaxLengthInput;

    @BeforeEach
    void setUp() {
        operation = new StringLengthBetweenOperator();
    }

    @Test
    @DisplayName("should return true if the string length is within the bounds")
    void testExecute_WhenLengthIsBetween_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockStringInput, mockMinLengthInput, mockMaxLengthInput));
        when(mockStringInput.getCalculatedResult()).thenReturn("StayInSync"); // Länge 10
        when(mockMinLengthInput.getCalculatedResult()).thenReturn(5);
        when(mockMaxLengthInput.getCalculatedResult()).thenReturn(15);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if the string length is outside the bounds")
    void testExecute_WhenLengthIsOutside_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockStringInput, mockMinLengthInput, mockMaxLengthInput));
        when(mockStringInput.getCalculatedResult()).thenReturn("StayInSync"); // Länge 10
        when(mockMinLengthInput.getCalculatedResult()).thenReturn(15);
        when(mockMaxLengthInput.getCalculatedResult()).thenReturn(20);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 3")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockStringInput, mockMinLengthInput)); // Nur zwei Inputs

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