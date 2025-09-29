package predicates.array_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.array_predicates.LengthEqualsOperator;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the LengthEqualsOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: ARRAY_LENGTH_EQUALS")
public class LengthEqualsOperatorTest {

    private LengthEqualsOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockArrayInput;
    @Mock
    private Node mockLengthInput;

    @BeforeEach
    void setUp() {
        operation = new LengthEqualsOperator();
    }

    // ===== BASIC FUNCTIONALITY TESTS =====

    @Test
    @DisplayName("should return true if the array length is equal to the number")
    void testExecute_WhenLengthIsEqual_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c")); // Länge 3
        when(mockLengthInput.getCalculatedResult()).thenReturn(3);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if the array length is not equal to the number")
    void testExecute_WhenLengthIsNotEqual_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c")); // Länge 3
        when(mockLengthInput.getCalculatedResult()).thenReturn(5);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return true for empty collection with zero length")
    void testExecute_WhenEmptyCollectionAndZero_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of()); // Länge 0
        when(mockLengthInput.getCalculatedResult()).thenReturn(0);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false for empty collection with non-zero length")
    void testExecute_WhenEmptyCollectionAndNonZero_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of()); // Länge 0
        when(mockLengthInput.getCalculatedResult()).thenReturn(1);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return true for single element collection")
    void testExecute_WhenSingleElementCollection_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("single")); // Länge 1
        when(mockLengthInput.getCalculatedResult()).thenReturn(1);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    // ===== ARRAY HANDLING TESTS =====

    @Test
    @DisplayName("should handle string arrays correctly")
    void testExecute_WithStringArray_ShouldWork() {
        // ARRANGE
        String[] stringArray = {"apple", "banana", "cherry", "date"};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(stringArray);
        when(mockLengthInput.getCalculatedResult()).thenReturn(4);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle integer arrays correctly")
    void testExecute_WithIntegerArray_ShouldWork() {
        // ARRANGE
        Integer[] intArray = {1, 2, 3, 4, 5};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(intArray);
        when(mockLengthInput.getCalculatedResult()).thenReturn(5);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle primitive int arrays correctly")
    void testExecute_WithPrimitiveIntArray_ShouldWork() {
        // ARRANGE
        int[] primitiveArray = {10, 20, 30};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(primitiveArray);
        when(mockLengthInput.getCalculatedResult()).thenReturn(3);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false for primitive array with wrong length")
    void testExecute_WithPrimitiveArrayWrongLength_ShouldReturnFalse() {
        // ARRANGE
        int[] primitiveArray = {10, 20, 30};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(primitiveArray);
        when(mockLengthInput.getCalculatedResult()).thenReturn(5);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should handle empty arrays correctly")
    void testExecute_WithEmptyArray_ShouldWork() {
        // ARRANGE
        String[] emptyArray = {};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(emptyArray);
        when(mockLengthInput.getCalculatedResult()).thenReturn(0);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    // ===== COLLECTION TYPE TESTS =====

    @Test
    @DisplayName("should handle ArrayList correctly")
    void testExecute_WithArrayList_ShouldWork() {
        // ARRANGE
        List<String> arrayList = new ArrayList<>();
        arrayList.add("x");
        arrayList.add("y");

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(arrayList);
        when(mockLengthInput.getCalculatedResult()).thenReturn(2);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle HashSet correctly")
    void testExecute_WithHashSet_ShouldWork() {
        // ARRANGE
        Set<String> hashSet = new HashSet<>();
        hashSet.add("alpha");
        hashSet.add("beta");
        hashSet.add("gamma");

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(hashSet);
        when(mockLengthInput.getCalculatedResult()).thenReturn(3);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    // ===== NUMBER TYPE TESTS =====

    @Test
    @DisplayName("should handle different number types - Double")
    void testExecute_WithDoubleLength_ShouldWork() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c"));
        when(mockLengthInput.getCalculatedResult()).thenReturn(3.0); // Double

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // 3.0 wird zu int 3 konvertiert
    }

    @Test
    @DisplayName("should handle different number types - Float")
    void testExecute_WithFloatLength_ShouldWork() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b"));
        when(mockLengthInput.getCalculatedResult()).thenReturn(2.0f); // Float

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle different number types - Long")
    void testExecute_WithLongLength_ShouldWork() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c", "d"));
        when(mockLengthInput.getCalculatedResult()).thenReturn(4L); // Long

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should truncate decimal values")
    void testExecute_WithDecimalLength_ShouldTruncate() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c"));
        when(mockLengthInput.getCalculatedResult()).thenReturn(3.9); // Wird zu 3 konvertiert

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    // ===== NEGATIVE NUMBER TESTS =====

    @Test
    @DisplayName("should handle negative expected length")
    void testExecute_WithNegativeLength_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c"));
        when(mockLengthInput.getCalculatedResult()).thenReturn(-1);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // Array-Länge kann nie negativ sein
    }

    @Test
    @DisplayName("should handle zero expected length with non-empty array")
    void testExecute_WithZeroLengthNonEmptyArray_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c"));
        when(mockLengthInput.getCalculatedResult()).thenReturn(0);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ===== NULL HANDLING TESTS =====

    @Test
    @DisplayName("should return false when array input is null")
    void testExecute_WhenArrayIsNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(null);
        when(mockLengthInput.getCalculatedResult()).thenReturn(3);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when length input is null")
    void testExecute_WhenLengthIsNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c"));
        when(mockLengthInput.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when both inputs are null")
    void testExecute_WhenBothInputsAreNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(null);
        when(mockLengthInput.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ===== INVALID INPUT TYPE TESTS =====

    @Test
    @DisplayName("should return false when length input is not a number")
    void testExecute_WithNonNumberLength_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c"));
        when(mockLengthInput.getCalculatedResult()).thenReturn("not a number");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when length input is boolean")
    void testExecute_WithBooleanLength_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c"));
        when(mockLengthInput.getCalculatedResult()).thenReturn(true);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when array input is not array or collection")
    void testExecute_WithInvalidArrayType_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn("not an array or collection");
        when(mockLengthInput.getCalculatedResult()).thenReturn(3);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when array input is a number")
    void testExecute_WithNumberAsArray_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(42);
        when(mockLengthInput.getCalculatedResult()).thenReturn(3);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when array input is boolean")
    void testExecute_WithBooleanAsArray_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(false);
        when(mockLengthInput.getCalculatedResult()).thenReturn(1);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ===== LARGE NUMBER TESTS =====

    @Test
    @DisplayName("should handle large collections correctly")
    void testExecute_WithLargeCollection_ShouldWork() {
        // ARRANGE
        List<Integer> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add(i);
        }

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(largeList);
        when(mockLengthInput.getCalculatedResult()).thenReturn(1000);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle very large expected length")
    void testExecute_WithVeryLargeExpectedLength_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c"));
        when(mockLengthInput.getCalculatedResult()).thenReturn(Integer.MAX_VALUE);

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
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput));
        when(mockLogicNode.getName()).thenReturn("TestNode");

        // ACT & ASSERT
        OperatorValidationException exception = assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });

        assertTrue(exception.getMessage().contains("TestNode"));
        assertTrue(exception.getMessage().contains("requires exactly 2 inputs"));
        assertTrue(exception.getMessage().contains("Array length comparison"));
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
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput, mockArrayInput));
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
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockLengthInput));

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