package predicates.string_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.string_predicates.RegexMatchOperator;
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
 * Unit tests for the RegexMatchOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: REGEX_MATCH")
public class RegexMatchOperatorTest {

    private RegexMatchOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockInputNode1; // Der Input, der den zu prüfenden String liefert
    @Mock
    private Node mockInputNode2; // Der Input, der das Regex-Muster liefert

    @BeforeEach
    void setUp() {
        operation = new RegexMatchOperator();
    }

    @Test
    @DisplayName("should return true if the string matches the regex pattern")
    void testExecute_WhenStringMatchesRegex_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        // Ein String, der auf das Muster "zwei Großbuchstaben, vier Zahlen" passt
        when(mockInputNode1.getCalculatedResult()).thenReturn("SN1234");
        when(mockInputNode2.getCalculatedResult()).thenReturn("^[A-Z]{2}[0-9]{4}$");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if the string does not match the regex pattern")
    void testExecute_WhenStringDoesNotMatchRegex_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn("Invalid-SN");
        when(mockInputNode2.getCalculatedResult()).thenReturn("^[A-Z]{2}[0-9]{4}$");

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