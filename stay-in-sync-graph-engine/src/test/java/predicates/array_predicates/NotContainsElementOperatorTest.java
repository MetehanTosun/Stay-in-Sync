package predicates.array_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.array_predicates.NotContainsElementOperator;
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
 * Unit tests for the NotContainsElementOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: NOT_CONTAINS_ELEMENT")
public class NotContainsElementOperatorTest {

    private NotContainsElementOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockArrayInput;
    @Mock
    private Node mockElementInput;

    @BeforeEach
    void setUp() {
        operation = new NotContainsElementOperator();
    }

    // ===== BASIC FUNCTIONALITY TESTS =====

    @Test
    @DisplayName("should return true if the array does not contain the element")
    void testExecute_WhenArrayDoesNotContainElement_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c"));
        when(mockElementInput.getCalculatedResult()).thenReturn("d");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if the array contains the element")
    void testExecute_WhenArrayContainsElement_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c"));
        when(mockElementInput.getCalculatedResult()).thenReturn("b");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when element is at first position")
    void testExecute_WhenElementIsFirst_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("target", "b", "c"));
        when(mockElementInput.getCalculatedResult()).thenReturn("target");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when element is at last position")
    void testExecute_WhenElementIsLast_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b", "target"));
        when(mockElementInput.getCalculatedResult()).thenReturn("target");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return true when searching in empty collection")
    void testExecute_WhenEmptyCollection_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of());
        when(mockElementInput.getCalculatedResult()).thenReturn("anything");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Empty collection does not contain anything
    }

    // ===== NULL HANDLING TESTS =====

    @Test
    @DisplayName("should return false when array input is null")
    void testExecute_WhenArrayIsNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(null);
        when(mockElementInput.getCalculatedResult()).thenReturn("element");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // Cannot prove element is not in null collection
    }

    @Test
    @DisplayName("should handle null element search in collection")
    void testExecute_WhenSearchingForNull_ShouldWork() {
        // ARRANGE
        List<String> listWithNull = new ArrayList<>();
        listWithNull.add("a");
        listWithNull.add(null);
        listWithNull.add("c");

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(listWithNull);
        when(mockElementInput.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // Collection contains null, so NOT contains is false
    }

    @Test
    @DisplayName("should return true when searching for null in collection without null")
    void testExecute_WhenSearchingForNullNotPresent_ShouldReturnTrue() {
        // ARRANGE
        List<String> listWithoutNull = new ArrayList<>();
        listWithoutNull.add("a");
        listWithoutNull.add("b");
        listWithoutNull.add("c");

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(listWithoutNull);
        when(mockElementInput.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Collection does not contain null
    }

    // ===== ARRAY HANDLING TESTS =====

    @Test
    @DisplayName("should handle string arrays correctly")
    void testExecute_WithStringArray_ShouldWork() {
        // ARRANGE
        String[] stringArray = {"apple", "banana", "cherry"};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(stringArray);
        when(mockElementInput.getCalculatedResult()).thenReturn("grape");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // grape is not in the array
    }

    @Test
    @DisplayName("should handle string arrays with match")
    void testExecute_WithStringArrayMatch_ShouldReturnFalse() {
        // ARRANGE
        String[] stringArray = {"apple", "banana", "cherry"};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(stringArray);
        when(mockElementInput.getCalculatedResult()).thenReturn("banana");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // banana is in the array
    }

    @Test
    @DisplayName("should handle integer arrays correctly")
    void testExecute_WithIntegerArray_ShouldWork() {
        // ARRANGE
        Integer[] intArray = {1, 2, 3, 4, 5};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(intArray);
        when(mockElementInput.getCalculatedResult()).thenReturn(6);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // 6 is not in the array
    }

    @Test
    @DisplayName("should handle primitive int arrays correctly")
    void testExecute_WithPrimitiveIntArray_ShouldWork() {
        // ARRANGE
        int[] primitiveArray = {10, 20, 30, 40};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(primitiveArray);
        when(mockElementInput.getCalculatedResult()).thenReturn(25);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // 25 is not in the array
    }

    @Test
    @DisplayName("should return false when element found in primitive array")
    void testExecute_WithPrimitiveArrayMatch_ShouldReturnFalse() {
        // ARRANGE
        int[] primitiveArray = {10, 20, 30, 40};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(primitiveArray);
        when(mockElementInput.getCalculatedResult()).thenReturn(20);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // 20 is in the array
    }

    @Test
    @DisplayName("should handle empty arrays correctly")
    void testExecute_WithEmptyArray_ShouldReturnTrue() {
        // ARRANGE
        String[] emptyArray = {};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(emptyArray);
        when(mockElementInput.getCalculatedResult()).thenReturn("anything");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Empty array does not contain anything
    }

    @Test
    @DisplayName("should handle null values in arrays")
    void testExecute_WithNullInArray_ShouldWork() {
        // ARRANGE
        Object[] arrayWithNull = {"a", null, "c"};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(arrayWithNull);
        when(mockElementInput.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // Array contains null
    }

    @Test
    @DisplayName("should handle null values in arrays when searching for non-null")
    void testExecute_WithNullInArraySearchingNonNull_ShouldReturnTrue() {
        // ARRANGE
        Object[] arrayWithNull = {"a", null, "c"};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(arrayWithNull);
        when(mockElementInput.getCalculatedResult()).thenReturn("d");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Array does not contain "d"
    }

    // ===== COLLECTION TYPE TESTS =====

    @Test
    @DisplayName("should handle different collection types - ArrayList")
    void testExecute_WithArrayList_ShouldWork() {
        // ARRANGE
        List<String> arrayList = new ArrayList<>();
        arrayList.add("x");
        arrayList.add("y");
        arrayList.add("z");

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(arrayList);
        when(mockElementInput.getCalculatedResult()).thenReturn("w");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // "w" is not in the list
    }

    @Test
    @DisplayName("should handle different collection types - HashSet")
    void testExecute_WithHashSet_ShouldWork() {
        // ARRANGE
        Set<String> hashSet = new HashSet<>();
        hashSet.add("alpha");
        hashSet.add("beta");
        hashSet.add("gamma");

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(hashSet);
        when(mockElementInput.getCalculatedResult()).thenReturn("delta");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // "delta" is not in the set
    }

    @Test
    @DisplayName("should handle HashSet with match")
    void testExecute_WithHashSetMatch_ShouldReturnFalse() {
        // ARRANGE
        Set<String> hashSet = new HashSet<>();
        hashSet.add("alpha");
        hashSet.add("beta");
        hashSet.add("gamma");

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(hashSet);
        when(mockElementInput.getCalculatedResult()).thenReturn("beta");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // "beta" is in the set
    }

    // ===== INVALID INPUT TYPE TESTS =====

    @Test
    @DisplayName("should return false when first input is not array or collection")
    void testExecute_WithInvalidInputType_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn("not an array or collection");
        when(mockElementInput.getCalculatedResult()).thenReturn("element");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // Cannot prove element is not in invalid type
    }

    @Test
    @DisplayName("should return false when first input is a number")
    void testExecute_WithNumberInput_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(42);
        when(mockElementInput.getCalculatedResult()).thenReturn("element");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when first input is a boolean")
    void testExecute_WithBooleanInput_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(true);
        when(mockElementInput.getCalculatedResult()).thenReturn("element");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ===== DATA TYPE MATCHING TESTS =====

    @Test
    @DisplayName("should handle type matching correctly - strings")
    void testExecute_WithStringTypeMatching_ShouldWork() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("1", "2", "3"));
        when(mockElementInput.getCalculatedResult()).thenReturn("4");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // "4" is not in the collection
    }

    @Test
    @DisplayName("should handle type mismatch correctly - string vs integer")
    void testExecute_WithTypeMismatch_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("1", "2", "3"));
        when(mockElementInput.getCalculatedResult()).thenReturn(2); // Integer 2 vs String "2"

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Integer 2 is not equal to String "2"
    }

    @Test
    @DisplayName("should handle mixed types in collections")
    void testExecute_WithMixedTypes_ShouldWork() {
        // ARRANGE
        List<Object> mixedList = new ArrayList<>();
        mixedList.add("string");
        mixedList.add(42);
        mixedList.add(true);
        mixedList.add(3.14);

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(mixedList);
        when(mockElementInput.getCalculatedResult()).thenReturn(99);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // 99 is not in the mixed list
    }

    @Test
    @DisplayName("should return false when mixed types contain element")
    void testExecute_WithMixedTypesMatch_ShouldReturnFalse() {
        // ARRANGE
        List<Object> mixedList = new ArrayList<>();
        mixedList.add("string");
        mixedList.add(42);
        mixedList.add(true);
        mixedList.add(3.14);

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(mixedList);
        when(mockElementInput.getCalculatedResult()).thenReturn(42);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // 42 is in the mixed list
    }

    // ===== DUPLICATE ELEMENTS TESTS =====

    @Test
    @DisplayName("should handle duplicate elements correctly")
    void testExecute_WithDuplicates_ShouldWork() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c", "b", "d"));
        when(mockElementInput.getCalculatedResult()).thenReturn("e");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // "e" is not in the list
    }

    @Test
    @DisplayName("should return false when searching for duplicate element")
    void testExecute_WithDuplicatesMatch_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));
        when(mockArrayInput.getCalculatedResult()).thenReturn(List.of("a", "b", "c", "b", "d"));
        when(mockElementInput.getCalculatedResult()).thenReturn("b");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result); // "b" is in the list (even though duplicated)
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
        assertTrue(exception.getMessage().contains("NOT_CONTAINS_ELEMENT"));
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
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput, mockArrayInput));
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
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockArrayInput, mockElementInput));

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