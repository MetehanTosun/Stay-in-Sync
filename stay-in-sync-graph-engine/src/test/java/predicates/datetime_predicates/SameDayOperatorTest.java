package predicates.datetime_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.datetime_predicates.SameDayOperator;
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
 * Unit tests for the SameDayOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: SAME_DAY")
public class SameDayOperatorTest {

    private SameDayOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockInputNode1;
    @Mock
    private Node mockInputNode2;

    @BeforeEach
    void setUp() {
        operation = new SameDayOperator();
    }

    @Test
    @DisplayName("should return true if both dates are on the same calendar day")
    void testExecute_WhenDatesAreOnSameDay_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        // Gleicher Tag, unterschiedliche Uhrzeit und Zeitzone
        when(mockInputNode1.getCalculatedResult()).thenReturn("2025-09-15T08:00:00+02:00[Europe/Berlin]");
        when(mockInputNode2.getCalculatedResult()).thenReturn("2025-09-15T23:00:00Z"); // Z = UTC

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if dates are on different calendar days")
    void testExecute_WhenDatesAreOnDifferentDays_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn("2025-09-15T23:59:59Z");
        when(mockInputNode2.getCalculatedResult()).thenReturn("2025-09-16T00:00:00Z");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 2")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));

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