package predicates.general_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.general_predicates.EqualsOperator;
import de.unistuttgart.graphengine.nodes.ConstantNode;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the EqualsOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: EQUALS")
public class EqualsOperatorTest {

    private EqualsOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockInputNode1;
    @Mock
    private Node mockInputNode2;
    @Mock
    private Node mockInputNode3;
    @Mock
    private ConstantNode mockConstantNode1;
    @Mock
    private ConstantNode mockConstantNode2;

    @BeforeEach
    void setUp() {
        operation = new EqualsOperator();
    }

    @Test
    @DisplayName("should return true when all input values are equal")
    void testExecute_WhenAllInputsAreEqual_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));
        when(mockInputNode1.getCalculatedResult()).thenReturn("test");
        when(mockInputNode2.getCalculatedResult()).thenReturn("test");
        when(mockInputNode3.getCalculatedResult()).thenReturn("test");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when at least one input value is different")
    void testExecute_WhenOneInputIsDifferent_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));
        when(mockInputNode1.getCalculatedResult()).thenReturn("test");
        when(mockInputNode2.getCalculatedResult()).thenReturn("test");
        when(mockInputNode3.getCalculatedResult()).thenReturn("different");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when comparing different types (e.g., number and string)")
    void testExecute_WhenComparingDifferentTypes_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(123);
        when(mockInputNode2.getCalculatedResult()).thenReturn("123");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should handle null values correctly")
    void testExecute_WithNullValues_ShouldBehaveCorrectly() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));

        // Fall 1: Beide sind null -> true
        when(mockInputNode1.getCalculatedResult()).thenReturn(null);
        when(mockInputNode2.getCalculatedResult()).thenReturn(null);
        Object result1 = operation.execute(mockLogicNode, null);

        // Fall 2: Einer ist null -> false
        when(mockInputNode1.getCalculatedResult()).thenReturn("not null");
        when(mockInputNode2.getCalculatedResult()).thenReturn(null);
        Object result2 = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result1, "Two nulls should be considered equal.");
        assertFalse((Boolean) result2, "A value and a null should not be considered equal.");
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @DisplayName("should throw exception if input count is less than 2")
    void testValidateNode_WithLessThanTwoInputs_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));

        when(mockLogicNode.getInputNodes()).thenReturn(Collections.emptyList());
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));

        when(mockLogicNode.getInputNodes()).thenReturn(null);
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should pass validation with zero or one ConstantNode")
    void testValidateNode_WithValidConstantNodeCount_ShouldPass() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2)); // 0 ConstantNodes
        assertDoesNotThrow(() -> operation.validateNode(mockLogicNode));

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockConstantNode1)); // 1 ConstantNode
        assertDoesNotThrow(() -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should throw exception with more than one ConstantNode")
    void testValidateNode_WithTooManyConstantNodes_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockConstantNode1, mockConstantNode2)); // 2 ConstantNodes

        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should declare its return type as Boolean")
    void getReturnType_ShouldReturnBooleanClass() {
        assertEquals(Boolean.class, operation.getReturnType());
    }
}