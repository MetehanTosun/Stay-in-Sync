package predicates.datetime_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.datetime_predicates.AgeGreaterThanOperator;
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
 * Unit tests for the AgeGreaterThanOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: AGE_GREATER_THAN")
public class AgeGreaterThanOperatorTest {

    private AgeGreaterThanOperator operation;

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
        operation = new AgeGreaterThanOperator();
    }

    @Test
    @DisplayName("should return true if the date's age is greater than the specified duration")
    void testExecute_WhenAgeIsGreater_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockDateInput, mockAmountInput, mockUnitInput));
        // Ein Datum, das 35 Tage in der Vergangenheit liegt
        when(mockDateInput.getCalculatedResult()).thenReturn(ZonedDateTime.now().minusDays(35).toString());
        when(mockAmountInput.getCalculatedResult()).thenReturn(30);
        when(mockUnitInput.getCalculatedResult()).thenReturn("DAYS");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if the date's age is not greater than the specified duration")
    void testExecute_WhenAgeIsNotGreater_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockDateInput, mockAmountInput, mockUnitInput));
        // Ein Datum, das 10 Tage in der Vergangenheit liegt
        when(mockDateInput.getCalculatedResult()).thenReturn(ZonedDateTime.now().minusDays(10).toString());
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
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockDateInput, mockAmountInput)); // Nur zwei Inputs

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