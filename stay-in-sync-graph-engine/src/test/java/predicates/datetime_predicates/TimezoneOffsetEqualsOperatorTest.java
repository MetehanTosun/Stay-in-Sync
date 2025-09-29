package predicates.datetime_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.datetime_predicates.TimezoneOffsetEqualsOperator;
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
 * Unit tests for the TimezoneOffsetEqualsOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: TIMEZONE_OFFSET_EQUALS")
public class TimezoneOffsetEqualsOperatorTest {

    private TimezoneOffsetEqualsOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockDateInput;
    @Mock
    private Node mockOffsetInput;

    @BeforeEach
    void setUp() {
        operation = new TimezoneOffsetEqualsOperator();
    }

    @Test
    @DisplayName("should return true if the date's timezone offset in minutes matches the number")
    void testExecute_WhenOffsetMatches_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockDateInput, mockOffsetInput));
        // CET (Central European Time) is UTC+1, which is 60 minutes.
        when(mockDateInput.getCalculatedResult()).thenReturn("2025-01-15T10:00:00+01:00[Europe/Berlin]");
        when(mockOffsetInput.getCalculatedResult()).thenReturn(60);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if the date's timezone offset does not match")
    void testExecute_WhenOffsetDoesNotMatch_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockDateInput, mockOffsetInput));
        // UTC offset is 0 minutes.
        when(mockDateInput.getCalculatedResult()).thenReturn("2025-09-15T12:00:00Z");
        when(mockOffsetInput.getCalculatedResult()).thenReturn(120); // Expecting UTC+2

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