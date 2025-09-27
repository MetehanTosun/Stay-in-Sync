package predicates.array_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.array_predicates.ContainsAllOperator;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the ContainsAllOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: CONTAINS_ALL")
public class ContainsAllOperatorTest {

    private ContainsAllOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockSourceListInput;
    @Mock
    private Node mockCheckListInput;

    @BeforeEach
    void setUp() {
        operation = new ContainsAllOperator();
    }

    @Test
    @DisplayName("should return true if the source list contains all elements from the check list")
    void testExecute_WhenSourceContainsAll_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c", "d"));
        when(mockCheckListInput.getCalculatedResult()).thenReturn(List.of("b", "d"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if the source list is missing any element from the check list")
    void testExecute_WhenSourceIsMissingElements_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c", "d"));
        when(mockCheckListInput.getCalculatedResult()).thenReturn(List.of("b", "e")); // "e" is missing

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when source input is null")
    void testExecute_WhenSourceIsNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn(null);
        when(mockCheckListInput.getCalculatedResult()).thenReturn(List.of("a", "b"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when reference input is null")
    void testExecute_WhenReferenceIsNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn(List.of("a", "b"));
        when(mockCheckListInput.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when both inputs are null")
    void testExecute_WhenBothInputsAreNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn(null);
        when(mockCheckListInput.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should handle arrays as input - source as array, reference as list")
    void testExecute_WithSourceArray_ShouldWork() {
        // ARRANGE
        String[] sourceArray = {"a", "b", "c", "d"};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn(sourceArray);
        when(mockCheckListInput.getCalculatedResult()).thenReturn(List.of("b", "d"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle arrays as input - both inputs as arrays")
    void testExecute_WithBothArrays_ShouldWork() {
        // ARRANGE
        String[] sourceArray = {"a", "b", "c", "d"};
        String[] referenceArray = {"b", "d"};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn(sourceArray);
        when(mockCheckListInput.getCalculatedResult()).thenReturn(referenceArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle primitive arrays")
    void testExecute_WithPrimitiveArray_ShouldWork() {
        // ARRANGE
        int[] sourceArray = {1, 2, 3, 4};
        Integer[] referenceArray = {2, 4};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn(sourceArray);
        when(mockCheckListInput.getCalculatedResult()).thenReturn(referenceArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle empty arrays")
    void testExecute_WithEmptyArrays_ShouldWork() {
        // ARRANGE
        String[] sourceArray = {};
        String[] referenceArray = {};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn(sourceArray);
        when(mockCheckListInput.getCalculatedResult()).thenReturn(referenceArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when source is not a collection or array")
    void testExecute_WithInvalidSourceType_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn("not a collection");
        when(mockCheckListInput.getCalculatedResult()).thenReturn(List.of("a", "b"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when reference is not a collection or array")
    void testExecute_WithInvalidReferenceType_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn(List.of("a", "b"));
        when(mockCheckListInput.getCalculatedResult()).thenReturn(123);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when both inputs have invalid types")
    void testExecute_WithBothInvalidTypes_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn("not a collection");
        when(mockCheckListInput.getCalculatedResult()).thenReturn(123);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return true when both collections are empty")
    void testExecute_WithBothEmpty_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn(List.of());
        when(mockCheckListInput.getCalculatedResult()).thenReturn(List.of());

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true when reference is empty but source is not")
    void testExecute_WithEmptyReference_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c"));
        when(mockCheckListInput.getCalculatedResult()).thenReturn(List.of());

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // containsAll returns true for empty collections
    }

    @Test
    @DisplayName("should return false when source is empty but reference is not")
    void testExecute_WithEmptySource_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn(List.of());
        when(mockCheckListInput.getCalculatedResult()).thenReturn(List.of("a", "b"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should handle different collection types - Set as source")
    void testExecute_WithSetAsSource_ShouldWork() {
        // ARRANGE
        Set<String> sourceSet = new HashSet<>();
        sourceSet.add("a");
        sourceSet.add("b");
        sourceSet.add("c");
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn(sourceSet);
        when(mockCheckListInput.getCalculatedResult()).thenReturn(List.of("a", "c"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle duplicate elements correctly")
    void testExecute_WithDuplicates_ShouldWork() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));
        when(mockSourceListInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c", "b", "d"));
        when(mockCheckListInput.getCalculatedResult()).thenReturn(List.of("b", "b", "d"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 2")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput));
        when(mockLogicNode.getName()).thenReturn("TestNode");

        // ACT & ASSERT
        OperatorValidationException exception = assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });

        assertTrue(exception.getMessage().contains("TestNode"));
        assertTrue(exception.getMessage().contains("requires exactly 2 inputs"));
    }

    @Test
    @DisplayName("should throw exception when input list is null")
    void testValidateNode_WithNullInputs_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(null);
        when(mockLogicNode.getName()).thenReturn("TestNode");

        // ACT & ASSERT
        OperatorValidationException exception = assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });

        assertTrue(exception.getMessage().contains("TestNode"));
        assertTrue(exception.getMessage().contains("requires exactly 2 inputs"));
    }

    @Test
    @DisplayName("should throw exception when too many inputs provided")
    void testValidateNode_WithTooManyInputs_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput, mockSourceListInput));
        when(mockLogicNode.getName()).thenReturn("TestNode");

        // ACT & ASSERT
        OperatorValidationException exception = assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });

        assertTrue(exception.getMessage().contains("TestNode"));
        assertTrue(exception.getMessage().contains("requires exactly 2 inputs"));
    }

    @Test
    @DisplayName("should pass validation with exactly 2 inputs")
    void testValidateNode_WithCorrectInputCount_ShouldNotThrow() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceListInput, mockCheckListInput));

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