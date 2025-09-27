package predicates.datetime_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.datetime_predicates.WeekdayIsOperator;
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
 * Unit tests for the WeekdayIsOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: WEEKDAY_IS")
public class WeekdayIsOperatorTest {

    private WeekdayIsOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockDateInput;
    @Mock
    private Node mockWeekdayInput;

    @BeforeEach
    void setUp() {
        operation = new WeekdayIsOperator();
    }

    @Test
    @DisplayName("should return true if the date's weekday matches the number")
    void testExecute_WhenWeekdayMatches_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockDateInput, mockWeekdayInput));
        // Der 15. September 2025 ist ein Montag.
        when(mockDateInput.getCalculatedResult()).thenReturn("2025-09-15T12:00:00Z");
        when(mockWeekdayInput.getCalculatedResult()).thenReturn(1); // 1 = Montag

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if the date's weekday does not match the number")
    void testExecute_WhenWeekdayDoesNotMatch_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockDateInput, mockWeekdayInput));
        // Der 15. September 2025 ist ein Montag.
        when(mockDateInput.getCalculatedResult()).thenReturn("2025-09-15T12:00:00Z");
        when(mockWeekdayInput.getCalculatedResult()).thenReturn(5); // 5 = Freitag

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 2")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockDateInput));

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