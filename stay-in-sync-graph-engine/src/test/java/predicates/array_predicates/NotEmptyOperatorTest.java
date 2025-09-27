package predicates.array_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.array_predicates.NotEmptyOperator;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the NotEmptyOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: NOT_EMPTY")
public class NotEmptyOperatorTest {

    private NotEmptyOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockInputNode;

    @BeforeEach
    void setUp() {
        operation = new NotEmptyOperator();
    }

    // ===== BASIC FUNCTIONALITY TESTS =====

    @Test
    @DisplayName("should return true for non-empty collection")
    void testExecute_WhenCollectionNotEmpty_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(List.of("a", "b", "c"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false for empty collection")
    void testExecute_WhenCollectionEmpty_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(List.of());

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return true for single element collection")
    void testExecute_WhenSingleElement_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(List.of("single"));

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    // ===== ARRAY HANDLING TESTS =====

    @Test
    @DisplayName("should return true for non-empty string array")
    void testExecute_WithNonEmptyStringArray_ShouldReturnTrue() {
        // ARRANGE
        String[] stringArray = {"apple", "banana", "cherry"};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(stringArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false for empty string array")
    void testExecute_WithEmptyStringArray_ShouldReturnFalse() {
        // ARRANGE
        String[] emptyArray = {};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(emptyArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return true for non-empty integer array")
    void testExecute_WithNonEmptyIntegerArray_ShouldReturnTrue() {
        // ARRANGE
        Integer[] intArray = {1, 2, 3, 4, 5};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(intArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true for non-empty primitive int array")
    void testExecute_WithNonEmptyPrimitiveIntArray_ShouldReturnTrue() {
        // ARRANGE
        int[] primitiveArray = {10, 20, 30};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(primitiveArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false for empty primitive int array")
    void testExecute_WithEmptyPrimitiveIntArray_ShouldReturnFalse() {
        // ARRANGE
        int[] emptyArray = {};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(emptyArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return true for single element array")
    void testExecute_WithSingleElementArray_ShouldReturnTrue() {
        // ARRANGE
        String[] singleElementArray = {"only"};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(singleElementArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle arrays with null elements")
    void testExecute_WithArrayContainingNull_ShouldReturnTrue() {
        // ARRANGE
        Object[] arrayWithNull = {null, "element"};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(arrayWithNull);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Array has elements (even if one is null)
    }

    @Test
    @DisplayName("should handle array with only null elements")
    void testExecute_WithArrayOnlyNulls_ShouldReturnTrue() {
        // ARRANGE
        Object[] nullArray = {null, null, null};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(nullArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Array has elements (even if all are null)
    }

    // ===== COLLECTION TYPE TESTS =====

    @Test
    @DisplayName("should handle ArrayList correctly")
    void testExecute_WithArrayList_ShouldWork() {
        // ARRANGE
        List<String> arrayList = new ArrayList<>();
        arrayList.add("x");
        arrayList.add("y");
        arrayList.add("z");

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(arrayList);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle empty ArrayList correctly")
    void testExecute_WithEmptyArrayList_ShouldReturnFalse() {
        // ARRANGE
        List<String> emptyList = new ArrayList<>();

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(emptyList);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should handle HashSet correctly")
    void testExecute_WithHashSet_ShouldWork() {
        // ARRANGE
        Set<String> hashSet = new HashSet<>();
        hashSet.add("alpha");
        hashSet.add("beta");

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(hashSet);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle empty HashSet correctly")
    void testExecute_WithEmptyHashSet_ShouldReturnFalse() {
        // ARRANGE
        Set<String> emptySet = new HashSet<>();

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(emptySet);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should handle LinkedList correctly")
    void testExecute_WithLinkedList_ShouldWork() {
        // ARRANGE
        LinkedList<Integer> linkedList = new LinkedList<>();
        linkedList.add(1);
        linkedList.add(2);

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(linkedList);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle Vector correctly")
    void testExecute_WithVector_ShouldWork() {
        // ARRANGE
        Vector<String> vector = new Vector<>();
        vector.add("element1");

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(vector);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle collection with null elements")
    void testExecute_WithCollectionContainingNull_ShouldReturnTrue() {
        // ARRANGE
        List<String> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        listWithNull.add("element");

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(listWithNull);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Collection has elements (even if one is null)
    }

    @Test
    @DisplayName("should handle collection with only null elements")
    void testExecute_WithCollectionOnlyNulls_ShouldReturnTrue() {
        // ARRANGE
        List<String> nullList = new ArrayList<>();
        nullList.add(null);
        nullList.add(null);

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(nullList);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result); // Collection has elements (even if all are null)
    }

    // ===== NULL HANDLING TESTS =====

    @Test
    @DisplayName("should return false when input is null")
    void testExecute_WhenInputIsNull_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(null);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ===== INVALID INPUT TYPE TESTS =====

    @Test
    @DisplayName("should return false when input is not array or collection")
    void testExecute_WithInvalidInputType_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn("not an array or collection");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when input is a number")
    void testExecute_WithNumberInput_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(42);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when input is a boolean")
    void testExecute_WithBooleanInput_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(true);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false when input is an object")
    void testExecute_WithObjectInput_ShouldReturnFalse() {
        // ARRANGE
        Object someObject = new Object();
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(someObject);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ===== MIXED CONTENT TESTS =====

    @Test
    @DisplayName("should handle mixed types in collections")
    void testExecute_WithMixedTypesInCollection_ShouldReturnTrue() {
        // ARRANGE
        List<Object> mixedList = new ArrayList<>();
        mixedList.add("string");
        mixedList.add(42);
        mixedList.add(true);
        mixedList.add(3.14);

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(mixedList);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle large collections")
    void testExecute_WithLargeCollection_ShouldReturnTrue() {
        // ARRANGE
        List<Integer> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add(i);
        }

        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(largeList);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    // ===== VARIOUS ARRAY TYPES TESTS =====

    @Test
    @DisplayName("should handle boolean arrays")
    void testExecute_WithBooleanArray_ShouldWork() {
        // ARRANGE
        boolean[] boolArray = {true, false, true};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(boolArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle double arrays")
    void testExecute_WithDoubleArray_ShouldWork() {
        // ARRANGE
        double[] doubleArray = {1.1, 2.2, 3.3};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(doubleArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle char arrays")
    void testExecute_WithCharArray_ShouldWork() {
        // ARRANGE
        char[] charArray = {'a', 'b', 'c'};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(charArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should handle empty char array")
    void testExecute_WithEmptyCharArray_ShouldReturnFalse() {
        // ARRANGE
        char[] emptyCharArray = {};
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));
        when(mockInputNode.getCalculatedResult()).thenReturn(emptyCharArray);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ===== VALIDATION TESTS =====

    @Test
    @DisplayName("should throw exception if input count is not exactly 1")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode, mockInputNode));
        when(mockLogicNode.getName()).thenReturn("TestNode");

        // ACT & ASSERT
        OperatorValidationException exception = assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });

        assertTrue(exception.getMessage().contains("TestNode"));
        assertTrue(exception.getMessage().contains("requires exactly 1 input"));
        assertTrue(exception.getMessage().contains("NOT_EMPTY"));
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
        assertTrue(exception.getMessage().contains("requires exactly 1 input"));
    }

    @Test
    @DisplayName("should throw exception when no inputs provided")
    void testValidateNode_WithNoInputs_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of());
        when(mockLogicNode.getName()).thenReturn("TestNode");

        // ACT & ASSERT
        OperatorValidationException exception = assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });

        assertTrue(exception.getMessage().contains("TestNode"));
        assertTrue(exception.getMessage().contains("requires exactly 1 input"));
    }

    @Test
    @DisplayName("should pass validation with exactly 1 input")
    void testValidateNode_WithCorrectInputCount_ShouldNotThrow() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode));

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