package predicates.boolean_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.boolean_predicates.IsTrueOperator;
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
 * Unit tests for the IsTrueOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: IS_TRUE")
public class IsTrueOperatorTest {

    private IsTrueOperator operation;

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
        operation = new IsTrueOperator();
    }

    // ===== SINGLE INPUT TESTS =====

    @Test
    @DisplayName("should return true when single input is Boolean.TRUE")
    void testExecute_WhenSingleInputIsTrue_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.TRUE);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when single input is Boolean.FALSE")
    void testExecute_WhenSingleInputIsFalse_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.FALSE);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return true when single input is primitive true")
    void testExecute_WhenSingleInputIsPrimitiveTrue_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(true); // primitive boolean

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when single input is primitive false")
    void testExecute_WhenSingleInputIsPrimitiveFalse_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(false); // primitive boolean

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ===== MULTIPLE INPUT TESTS (AND-conjunction) =====

    @Test
    @DisplayName("should return true when all inputs are Boolean.TRUE")
    void testExecute_WhenAllInputsAreTrue_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.TRUE);
        when(mockInputNode2.getCalculatedResult()).thenReturn(Boolean.TRUE);
        when(mockInputNode3.getCalculatedResult()).thenReturn(Boolean.TRUE);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when one input is Boolean.FALSE and others are TRUE")
    void testExecute_WhenOneInputIsFalse_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.TRUE);
        when(mockInputNode2.getCalculatedResult()).thenReturn(Boolean.FALSE); // This one is false - Early Exit
        // mockInputNode3 wird nie aufgerufen wegen Early Exit

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when first input is Boolean.FALSE")
    void testExecute_WhenFirstInputIsFalse_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.FALSE); // First one fails - Early Exit
        // mockInputNode2 wird nie aufgerufen wegen Early Exit

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when last input is Boolean.FALSE")
    void testExecute_WhenLastInputIsFalse_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.TRUE);
        when(mockInputNode2.getCalculatedResult()).thenReturn(Boolean.FALSE); // Last one fails

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return true when all inputs are mix of Boolean.TRUE and primitive true")
    void testExecute_WhenMixOfTrueTypes_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.TRUE);
        when(mockInputNode2.getCalculatedResult()).thenReturn(true); // primitive
        when(mockInputNode3.getCalculatedResult()).thenReturn(Boolean.TRUE);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    // ===== NON-BOOLEAN INPUT TESTS =====

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
    @DisplayName("should return false when mix of true and non-boolean inputs")
    void testExecute_WhenMixOfTrueAndNonBoolean_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.TRUE);
        when(mockInputNode2.getCalculatedResult()).thenReturn("true"); // String, not Boolean - Early Exit
        // mockInputNode3 wird nie aufgerufen wegen Early Exit

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when mix of true and null inputs")
    void testExecute_WhenMixOfTrueAndNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.TRUE);
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
        when(mockInputNode1.getCalculatedResult()).thenReturn("true"); // Fails here - Early Exit
        // mockInputNode2 und mockInputNode3 werden nie aufgerufen

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ===== MANY INPUTS TESTS =====

    @Test
    @DisplayName("should handle many true inputs correctly")
    void testExecute_WhenManyTrueInputs_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3, mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.TRUE);
        when(mockInputNode2.getCalculatedResult()).thenReturn(true);
        when(mockInputNode3.getCalculatedResult()).thenReturn(Boolean.TRUE);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when many inputs with one false")
    void testExecute_WhenManyInputsWithOneFalse_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3, mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Boolean.TRUE);
        when(mockInputNode2.getCalculatedResult()).thenReturn(Boolean.FALSE); // This one fails - Early Exit
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

        assertTrue(exception.getMessage().contains("IS_TRUE"));
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

        assertTrue(exception.getMessage().contains("IS_TRUE"));
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