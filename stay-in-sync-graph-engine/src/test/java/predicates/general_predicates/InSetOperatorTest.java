package predicates.general_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.general_predicates.InSetOperator;
import de.unistuttgart.graphengine.nodes.ConstantNode;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the InSetOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: IN_SET")
public class InSetOperatorTest {

    private InSetOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockValueInput;
    @Mock
    private ConstantNode mockSetConstantNode;
    @Mock
    private Node mockNonConstantNode;

    @BeforeEach
    void setUp() {
        operation = new InSetOperator();
    }

    // ==================== EXECUTE TESTS ====================

    @Test
    @DisplayName("should return true when the value is present in the list")
    void testExecute_WhenValueIsInList_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueInput, mockSetConstantNode));
        when(mockValueInput.getCalculatedResult()).thenReturn("active");
        when(mockSetConstantNode.getCalculatedResult()).thenReturn(List.of("active", "pending", "archived"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true when the value is present in an array")
    void testExecute_WhenValueIsInArray_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueInput, mockSetConstantNode));
        when(mockValueInput.getCalculatedResult()).thenReturn(100);
        when(mockSetConstantNode.getCalculatedResult()).thenReturn(new Object[]{50, 100, 150});

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when the value is not present in the set")
    void testExecute_WhenValueIsNotInSet_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueInput, mockSetConstantNode));
        when(mockValueInput.getCalculatedResult()).thenReturn("deleted");
        when(mockSetConstantNode.getCalculatedResult()).thenReturn(List.of("active", "pending", "archived"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return true when checking for null and null is in the set")
    void testExecute_WhenCheckingForNullAndSetContainsNull_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueInput, mockSetConstantNode));
        when(mockValueInput.getCalculatedResult()).thenReturn(null);
        when(mockSetConstantNode.getCalculatedResult()).thenReturn(Arrays.asList("active", null, "archived"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when set input is null")
    void testExecute_WhenSetInputIsNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueInput, mockSetConstantNode));
        when(mockValueInput.getCalculatedResult()).thenReturn("active");
        when(mockSetConstantNode.getCalculatedResult()).thenReturn(null);
        // ACT
        Object result = operation.execute(mockLogicNode, null);
        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when set input is not a collection or array")
    void testExecute_WhenSetInputIsNotACollection_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueInput, mockSetConstantNode));
        when(mockValueInput.getCalculatedResult()).thenReturn("active");
        when(mockSetConstantNode.getCalculatedResult()).thenReturn("not a collection");
        // ACT
        Object result = operation.execute(mockLogicNode, null);
        // ASSERT
        assertFalse((Boolean) result);
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @DisplayName("should throw exception if input count is not exactly 2")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueInput)); // Nur 1
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueInput, mockSetConstantNode, mockValueInput)); // 3
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should throw exception if second input is not a ConstantNode")
    void testValidateNode_WhenSecondInputIsNotConstantNode_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueInput, mockNonConstantNode));
        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should throw exception if ConstantNode value is not an array")
    void testValidateNode_WhenConstantValueIsNotArray_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockValueInput, mockSetConstantNode));
        when(mockSetConstantNode.getValue()).thenReturn("not an array");
        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should declare its return type as Boolean")
    void getReturnType_ShouldReturnBooleanClass() {
        assertEquals(Boolean.class, operation.getReturnType());
    }
}