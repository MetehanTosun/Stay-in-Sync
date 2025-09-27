package predicates.datetime_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.datetime_predicates.MonthIsOperator;
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
 * Unit tests for the MonthIsOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: MONTH_IS")
public class MonthIsOperatorTest {

    private MonthIsOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockDateInput;
    @Mock
    private Node mockMonthInput;

    @BeforeEach
    void setUp() {
        operation = new MonthIsOperator();
    }

    @Test
    @DisplayName("should return true if the date's month matches the number")
    void testExecute_WhenMonthMatches_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockDateInput, mockMonthInput));
        // Datum im September
        when(mockDateInput.getCalculatedResult()).thenReturn("2025-09-15T12:00:00Z");
        when(mockMonthInput.getCalculatedResult()).thenReturn(9); // 9 = September

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if the date's month does not match the number")
    void testExecute_WhenMonthDoesNotMatch_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockDateInput, mockMonthInput));
        // Datum im September
        when(mockDateInput.getCalculatedResult()).thenReturn("2025-09-15T12:00:00Z");
        when(mockMonthInput.getCalculatedResult()).thenReturn(10); // 10 = Oktober

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