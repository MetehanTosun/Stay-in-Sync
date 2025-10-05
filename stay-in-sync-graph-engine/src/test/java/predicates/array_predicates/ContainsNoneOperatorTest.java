package predicates.array_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.array_predicates.ContainsNoneOperator;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the ContainsNoneOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: CONTAINS_NONE")
public class ContainsNoneOperatorTest {

    private ContainsNoneOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockSourceArrayNode;
    @Mock
    private Node mockSearchArrayNode;

    @BeforeEach
    void setUp() {
        operation = new ContainsNoneOperator();
    }

    // ===== BASIC FUNCTIONALITY TESTS =====

    @Test
    @DisplayName("should return true when source array contains none of the search elements")
    void testExecute_WhenSourceContainsNoElement_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(List.of("apple", "banana", "cherry"));
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(List.of("orange", "grape")); // None match

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when source array contains at least one search element")
    void testExecute_WhenSourceContainsAnyElement_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(List.of("apple", "banana", "cherry"));
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(List.of("banana", "orange")); // "banana" matches

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when source contains multiple matching elements")
    void testExecute_WhenSourceContainsMultipleElements_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(List.of(1, 2, 3, 4, 5));
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(List.of(3, 4)); // Both match

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when first element in reference matches")
    void testExecute_WithFirstElementMatch_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(List.of("a", "b", "c"));
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(List.of("a", "x", "y")); // First element matches

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // Should return false as soon as first match is found
    }

    @Test
    @DisplayName("should return false when last element in reference matches")
    void testExecute_WithLastElementMatch_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(List.of("a", "b", "c"));
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(List.of("x", "y", "c")); // Last element matches

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ===== EMPTY COLLECTION TESTS =====

    @Test
    @DisplayName("should return true when search array is empty")
    void testExecute_WhenSearchArrayIsEmpty_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(List.of("apple", "banana"));
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(List.of()); // Empty search = contains none

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Empty reference = no elements to find
    }

    @Test
    @DisplayName("should return true when source array is empty")
    void testExecute_WhenSourceArrayIsEmpty_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(List.of()); // Empty source contains none
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(List.of("apple", "banana"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Empty source can't contain anything
    }

    @Test
    @DisplayName("should return true when both arrays are empty")
    void testExecute_WhenBothArraysEmpty_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(List.of());
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(List.of());

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Empty reference collection = contains none
    }

    // ===== NULL HANDLING TESTS =====

    @Test
    @DisplayName("should return false when source input is null")
    void testExecute_WhenSourceIsNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(null);
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(List.of("test"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // Inherited behavior from AbstractSetComparisonOperator
    }

    @Test
    @DisplayName("should return false when reference input is null")
    void testExecute_WhenReferenceIsNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(List.of("apple", "banana"));
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // Inherited behavior from AbstractSetComparisonOperator
    }

    @Test
    @DisplayName("should return false when both inputs are null")
    void testExecute_WhenBothInputsAreNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(null);
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // Inherited behavior from AbstractSetComparisonOperator
    }

    // ===== ARRAY HANDLING TESTS =====

    @Test
    @DisplayName("should handle arrays as input - source as array, reference as list")
    void testExecute_WithSourceArray_ShouldWork() {
        // ARRANGE
        String[] sourceArray = {"apple", "banana", "cherry"};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(sourceArray);
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(List.of("grape", "orange")); // None match

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle arrays as input - both inputs as arrays")
    void testExecute_WithBothArrays_ShouldWork() {
        // ARRANGE
        String[] sourceArray = {"apple", "banana", "cherry"};
        String[] referenceArray = {"grape", "orange"};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(sourceArray);
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(referenceArray);

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
        Integer[] referenceArray = {5, 6, 7};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(sourceArray);
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(referenceArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // None match
    }

    @Test
    @DisplayName("should handle primitive arrays with match")
    void testExecute_WithPrimitiveArrayMatch_ShouldReturnFalse() {
        // ARRANGE
        int[] sourceArray = {1, 2, 3, 4};
        Integer[] referenceArray = {5, 2, 7};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(sourceArray);
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(referenceArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // 2 matches
    }

    @Test
    @DisplayName("should handle empty arrays")
    void testExecute_WithEmptyArrays_ShouldWork() {
        // ARRANGE
        String[] sourceArray = {};
        String[] referenceArray = {};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(sourceArray);
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(referenceArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Empty reference = contains none
    }

    // ===== INVALID INPUT TYPE TESTS =====

    @Test
    @DisplayName("should return false when source is not a collection or array")
    void testExecute_WithInvalidSourceType_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn("not a collection");
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(List.of("a", "b"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // Inherited behavior from AbstractSetComparisonOperator
    }

    @Test
    @DisplayName("should return false when reference is not a collection or array")
    void testExecute_WithInvalidReferenceType_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(List.of("a", "b"));
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(123);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // Inherited behavior from AbstractSetComparisonOperator
    }

    @Test
    @DisplayName("should return false when both inputs have invalid types")
    void testExecute_WithBothInvalidTypes_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn("not a collection");
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(123);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // Inherited behavior from AbstractSetComparisonOperator
    }

    // ===== COLLECTION TYPE TESTS =====

    @Test
    @DisplayName("should handle different collection types - Set as source")
    void testExecute_WithSetAsSource_ShouldWork() {
        // ARRANGE
        Set<String> sourceSet = new HashSet<>();
        sourceSet.add("apple");
        sourceSet.add("banana");
        sourceSet.add("cherry");
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(sourceSet);
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(List.of("grape", "orange")); // None match

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle duplicate elements correctly")
    void testExecute_WithDuplicates_ShouldWork() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(List.of("a", "b", "c", "b", "d"));
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(List.of("e", "f", "g"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // None match
    }

    @Test
    @DisplayName("should handle duplicate elements with match")
    void testExecute_WithDuplicatesMatch_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(List.of("a", "b", "c", "b", "d"));
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(List.of("b", "e", "f"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // "b" matches
    }

    @Test
    @DisplayName("should handle mixed data types correctly")
    void testExecute_WithMixedDataTypes_ShouldWork() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(Arrays.asList(1, "hello", 3.14, true));
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(List.of("world", false)); // None match

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when mixed data types match")
    void testExecute_WithMixedDataTypesMatch_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));
        when(mockSourceArrayNode.getCalculatedResult()).thenReturn(Arrays.asList(1, "hello", 3.14, true));
        when(mockSearchArrayNode.getCalculatedResult()).thenReturn(List.of("hello", false)); // "hello" matches

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ===== VALIDATION TESTS =====

    @Test
    @DisplayName("should throw exception if input count is not exactly 2")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode));
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
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode, mockSourceArrayNode));
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
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockSourceArrayNode, mockSearchArrayNode));

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