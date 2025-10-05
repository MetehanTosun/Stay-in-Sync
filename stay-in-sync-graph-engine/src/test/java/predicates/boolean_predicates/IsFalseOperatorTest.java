package predicates.boolean_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.boolean_predicates.IsFalseOperator;
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
 * Unit tests for the IsFalseOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: IS_FALSE")
public class IsFalseOperatorTest {

    private IsFalseOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockInputNode1;
    @Mock
    private Node mockInputNode2;
    @Mock
    private Node mockInputNode3;

    @BeforeEach
    void setUp() {
        operation = new IsFalseOperator();
    }

    // ===== SINGLE INPUT TESTS =====

    @Test
    @DisplayName("should return true when single input is Boolean.FALSE")
    void testExecute_WhenSingleInputIsFalse_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.FALSE);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when single input is Boolean.TRUE")
    void testExecute_WhenSingleInputIsTrue_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.TRUE);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return true when single input is primitive false")
    void testExecute_WhenSingleInputIsPrimitiveFalse_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(false); // primitive boolean

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when single input is primitive true")
    void testExecute_WhenSingleInputIsPrimitiveTrue_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(true); // primitive boolean

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ===== MULTIPLE INPUT TESTS (AND-conjunction) =====

    @Test
    @DisplayName("should return true when all inputs are Boolean.FALSE")
    void testExecute_WhenAllInputsAreFalse_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.FALSE);
        when(mockInputNode2.getCalculatedResult()).thenReturn(Boolean.FALSE);
        when(mockInputNode3.getCalculatedResult()).thenReturn(Boolean.FALSE);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when one input is Boolean.TRUE and others are FALSE")
    void testExecute_WhenOneInputIsTrue_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.FALSE);
        when(mockInputNode2.getCalculatedResult()).thenReturn(Boolean.TRUE); // This one is true
        // mockInputNode3 wird nie aufgerufen wegen Early Exit

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when first input is Boolean.TRUE")
    void testExecute_WhenFirstInputIsTrue_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.TRUE); // First one fails
        // mockInputNode2 wird nie aufgerufen wegen Early Exit

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when last input is Boolean.TRUE")
    void testExecute_WhenLastInputIsTrue_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.FALSE);
        when(mockInputNode2.getCalculatedResult()).thenReturn(Boolean.TRUE); // Last one fails

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return true when all inputs are mix of Boolean.FALSE and primitive false")
    void testExecute_WhenMixOfFalseTypes_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.FALSE);
        when(mockInputNode2.getCalculatedResult()).thenReturn(false); // primitive
        when(mockInputNode3.getCalculatedResult()).thenReturn(Boolean.FALSE);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    // ===== NON-BOOLEAN INPUT TESTS =====

    @Test
    @DisplayName("should return false when input is String 'false'")
    void testExecute_WhenInputIsStringFalse_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn("false"); // String, not Boolean

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when input is String 'true'")
    void testExecute_WhenInputIsStringTrue_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn("true"); // String, not Boolean

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when input is integer 0")
    void testExecute_WhenInputIsZero_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(0); // Integer, not Boolean

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when input is integer 1")
    void testExecute_WhenInputIsOne_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(1); // Integer, not Boolean

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when input is null")
    void testExecute_WhenInputIsNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when input is empty string")
    void testExecute_WhenInputIsEmptyString_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn("");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when input is object")
    void testExecute_WhenInputIsObject_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(new Object());

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ===== MIXED INPUT SCENARIOS =====

    @Test
    @DisplayName("should return false when mix of false and non-boolean inputs")
    void testExecute_WhenMixOfFalseAndNonBoolean_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.FALSE);
        when(mockInputNode2.getCalculatedResult()).thenReturn("false"); // String, not Boolean - fails here
        // mockInputNode3 wird nie aufgerufen wegen Early Exit

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when mix of false and null inputs")
    void testExecute_WhenMixOfFalseAndNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.FALSE);
        when(mockInputNode2.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when all inputs are non-boolean")
    void testExecute_WhenAllInputsNonBoolean_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));
        when(mockInputNode1.getCalculatedResult()).thenReturn("false"); // Fails here - Early Exit
        // mockInputNode2 und mockInputNode3 werden nie aufgerufen

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ===== MANY INPUTS TESTS =====

    @Test
    @DisplayName("should handle many false inputs correctly")
    void testExecute_WhenManyFalseInputs_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3, mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.FALSE);
        when(mockInputNode2.getCalculatedResult()).thenReturn(false);
        when(mockInputNode3.getCalculatedResult()).thenReturn(Boolean.FALSE);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when many inputs with one true")
    void testExecute_WhenManyInputsWithOneTrue_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3, mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.FALSE);
        when(mockInputNode2.getCalculatedResult()).thenReturn(Boolean.TRUE); // This one fails - Early Exit
        // mockInputNode3 wird nie aufgerufen wegen Early Exit

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ===== VALIDATION TESTS =====

    @Test
    @DisplayName("should throw exception if there are no inputs")
    void testValidateNode_WithNoInputs_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of()); // Empty list

        // ACT & ASSERT
        OperatorValidationException exception = assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });

        assertTrue(exception.getMessage().contains("IS_FALSE"));
        assertTrue(exception.getMessage().contains("requires at least 1 input"));
    }

    @Test
    @DisplayName("should throw exception when input list is null")
    void testValidateNode_WithNullInputs_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(null);

        // ACT & ASSERT
        OperatorValidationException exception = assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });

        assertTrue(exception.getMessage().contains("IS_FALSE"));
        assertTrue(exception.getMessage().contains("requires at least 1 input"));
    }

    @Test
    @DisplayName("should pass validation with exactly 1 input")
    void testValidateNode_WithOneInput_ShouldNotThrow() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            operation.validateNode(mockLogicNode);
        });
    }

    @Test
    @DisplayName("should pass validation with multiple inputs")
    void testValidateNode_WithMultipleInputs_ShouldNotThrow() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            operation.validateNode(mockLogicNode);
        });
    }

    @Test
    @DisplayName("should declare its return type as Boolean")
    void getReturnType_ShouldReturnBooleanClass() {
        assertEquals(Boolean.class, operation.getReturnType());
    }
}