package predicates.general_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.general_predicates.IsNotNullOperator;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.graphengine.nodes.ProviderNode;
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
 * Unit tests for the IsNotNullOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: IS_NOT_NULL")
public class IsNotNullOperatorTest {

    private IsNotNullOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private ProviderNode mockInputNode1;
    @Mock
    private ProviderNode mockInputNode2;
    @Mock
    private Node mockInvalidInputNode;

    @BeforeEach
    void setUp() {
        operation = new IsNotNullOperator();
    }

    @Test
    @DisplayName("should return true when the input value is not null")
    void testExecute_WhenInputIsNotNull_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn("some value");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when the input value is null")
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
    @DisplayName("should return true only if ALL input values are not null")
    void testExecute_WhenAllMultipleInputsAreNotNull_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn("value 1");
        when(mockInputNode2.getCalculatedResult()).thenReturn(123);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if any of multiple input values is null")
    void testExecute_WhenAnyOfMultipleInputsIsNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn("not null");
        when(mockInputNode2.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ==================== NODE VALIDATION TESTS ====================

    @Test
    @DisplayName("should throw exception when inputs list is null")
    void testValidateNode_WhenInputsListIsNull_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(null);

        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should throw exception when inputs list is empty")
    void testValidateNode_WhenInputsListIsEmpty_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(Collections.emptyList());

        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should throw exception when an input is not a ProviderNode")
    void testValidateNode_WhenInputIsNotProviderNode_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInvalidInputNode));

        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }


    @Test
    @DisplayName("should declare its return type as Boolean")
    void getReturnType_ShouldReturnBooleanClass() {
        assertEquals(Boolean.class, operation.getReturnType());
    }
}