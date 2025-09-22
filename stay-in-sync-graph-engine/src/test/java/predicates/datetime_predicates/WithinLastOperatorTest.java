package predicates.datetime_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.datetime_predicates.WithinLastOperator;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the WithinLastOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: WITHIN_LAST")
public class WithinLastOperatorTest {

    private WithinLastOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockDateInput;
    @Mock
    private Node mockAmountInput;
    @Mock
    private Node mockUnitInput;

    @BeforeEach
    void setUp() {
        operation = new WithinLastOperator();
    }

    @Test
    @DisplayName("should return true if the date is within the last specified duration")
    void testExecute_WhenDateIsWithinLastDuration_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockDateInput, mockAmountInput, mockUnitInput));
        // Ein Datum, das 10 Tage in der Vergangenheit liegt
        when(mockDateInput.getCalculatedResult()).thenReturn(ZonedDateTime.now().minusDays(10).toString());
        // Prüfe, ob es in den letzten 30 Tagen war
        when(mockAmountInput.getCalculatedResult()).thenReturn(30);
        when(mockUnitInput.getCalculatedResult()).thenReturn("DAYS");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if the date is outside the last specified duration")
    void testExecute_WhenDateIsOutsideLastDuration_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockDateInput, mockAmountInput, mockUnitInput));
        // Ein Datum, das 35 Tage in der Vergangenheit liegt
        when(mockDateInput.getCalculatedResult()).thenReturn(ZonedDateTime.now().minusDays(35).toString());
        // Prüfe, ob es in den letzten 30 Tagen war
        when(mockAmountInput.getCalculatedResult()).thenReturn(30);
        when(mockUnitInput.getCalculatedResult()).thenReturn("DAYS");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 3")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockDateInput, mockAmountInput));

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