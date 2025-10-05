package predicates.datetime_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.datetime_predicates.BetweenDatesOperator;
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
 * Unit tests for the BetweenDatesOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: BETWEEN_DATES")
public class BetweenDatesOperatorTest {

    private BetweenDatesOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockDateNode;
    @Mock
    private Node mockStartDateNode;
    @Mock
    private Node mockEndDateNode;

    @BeforeEach
    void setUp() {
        operation = new BetweenDatesOperator();
    }

    @Test
    @DisplayName("should return true if the date is within the bounds")
    void testExecute_WhenDateIsBetween_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockDateNode, mockStartDateNode, mockEndDateNode));
        when(mockDateNode.getCalculatedResult()).thenReturn("2025-09-15T12:00:00Z");
        when(mockStartDateNode.getCalculatedResult()).thenReturn("2025-09-15T10:00:00Z");
        when(mockEndDateNode.getCalculatedResult()).thenReturn("2025-09-15T14:00:00Z");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if the date is outside the bounds")
    void testExecute_WhenDateIsOutside_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockDateNode, mockStartDateNode, mockEndDateNode));
        when(mockDateNode.getCalculatedResult()).thenReturn("2025-09-15T09:00:00Z");
        when(mockStartDateNode.getCalculatedResult()).thenReturn("2025-09-15T10:00:00Z");
        when(mockEndDateNode.getCalculatedResult()).thenReturn("2025-09-15T14:00:00Z");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 3")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockDateNode, mockStartDateNode)); // Nur zwei Inputs

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