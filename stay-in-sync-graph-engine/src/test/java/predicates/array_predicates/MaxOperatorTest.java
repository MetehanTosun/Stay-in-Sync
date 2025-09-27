package predicates.array_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.array_predicates.MaxOperator;
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
 * Unit tests for the MaxOperator based on multiple array/collection inputs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: MAX")
public class MaxOperatorTest {

    private MaxOperator operation;

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
        operation = new MaxOperator();
    }

    @Test
    @DisplayName("should return the maximum value from multiple list/array inputs")
    void testExecute_WithMultipleCollectionInputs_ShouldReturnMaximum() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));

        // Input 1: [1, 5, 3] → max candidate: 5
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(1, 5, 3));
        // Input 2: [2, 8, 4] → max candidate: 8
        when(mockInputNode2.getCalculatedResult()).thenReturn(new Object[]{2, 8, 4});
        // Input 3: [6, 7] → max candidate: 7
        when(mockInputNode3.getCalculatedResult()).thenReturn(List.of(6, 7));

        // Overall maximum should be 8

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(8.0, (Double) result);
        });
    }

    @Test
    @DisplayName("should return correct maximum from single array input")
    void testExecute_WithSingleArrayInput_ShouldReturnMaximum() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(10, 3, 7, 1, 9)); // max = 10

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(10.0, (Double) result);
        });
    }

    @Test
    @DisplayName("should handle different numeric types correctly")
    void testExecute_WithDifferentNumericTypes_ShouldReturnMaximum() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(
                10,      // Integer
                15.5,    // Double (this should be max)
                12.2f,   // Float
                8L       // Long
        )); // max = 15.5

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(15.5, (Double) result);
        });
    }

    @Test
    @DisplayName("should ignore non-numeric values and find maximum of numeric ones")
    void testExecute_WithMixedTypes_ShouldIgnoreNonNumeric() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Arrays.asList(1, "text", 7, null)); // max candidate: 7
        when(mockInputNode2.getCalculatedResult()).thenReturn(List.of(5, 3)); // max candidate: 5
        // Overall max should be 7

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(7.0, (Double) result);
        });
    }

    @Test
    @DisplayName("should handle negative numbers correctly")
    void testExecute_WithNegativeNumbers_ShouldReturnMaximum() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(-5, -1, -10, -3)); // max = -1

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(-1.0, (Double) result);
        });
    }

    @Test
    @DisplayName("should handle mix of positive and negative numbers")
    void testExecute_WithMixedSignNumbers_ShouldReturnMaximum() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(-5, 2, -1)); // max candidate: 2
        when(mockInputNode2.getCalculatedResult()).thenReturn(List.of(-10, 8, -3)); // max candidate: 8
        // Overall max should be 8

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(8.0, (Double) result);
        });
    }

    @Test
    @DisplayName("should return 0.0 if inputs are not collections or arrays")
    void testExecute_WithNonCollectionInput_ShouldReturnZero() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(123); // Single number, not array

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(0.0, (Double) result, "Should return 0.0 as the input is not a collection/array.");
        });
    }

    @Test
    @DisplayName("should return 0.0 when input node returns null")
    void testExecute_WithNullInput_ShouldReturnZero() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(null);

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(0.0, (Double) result);
        });
    }

    @Test
    @DisplayName("should return 0.0 for empty collections")
    void testExecute_WithEmptyCollections_ShouldReturnZero() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of());
        when(mockInputNode2.getCalculatedResult()).thenReturn(new Object[]{});

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(0.0, (Double) result);
        });
    }

    @Test
    @DisplayName("should return 0.0 when collections contain only non-numeric values")
    void testExecute_WithOnlyNonNumericValues_ShouldReturnZero() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Arrays.asList("text", true, null));

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(0.0, (Double) result);
        });
    }

    @Test
    @DisplayName("should handle mixed valid and invalid inputs")
    void testExecute_WithMixedInputs_ShouldFindMaxFromValidArrays() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(2, 4)); // Valid: max candidate 4
        when(mockInputNode2.getCalculatedResult()).thenReturn("not an array"); // Invalid: ignored
        when(mockInputNode3.getCalculatedResult()).thenReturn(List.of(6, 1)); // Valid: max candidate 6
        // Overall max should be 6

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(6.0, (Double) result);
        });
    }

    @Test
    @DisplayName("should handle single element arrays correctly")
    void testExecute_WithSingleElementArrays_ShouldReturnThatElement() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(42));
        when(mockInputNode2.getCalculatedResult()).thenReturn(List.of(17));
        // max should be 42

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(42.0, (Double) result);
        });
    }

    @Test
    @DisplayName("should handle decimal values correctly")
    void testExecute_WithDecimalValues_ShouldReturnCorrectMaximum() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(1.1, 1.9, 1.5)); // max = 1.9

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(1.9, (Double) result);
        });
    }

    @Test
    @DisplayName("should throw validation exception if there are no inputs")
    void testValidateNode_WithNoInputs_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of());
        when(mockLogicNode.getName()).thenReturn("TestMaxNode");

        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });
    }

    @Test
    @DisplayName("should throw validation exception if inputs list is null")
    void testValidateNode_WithNullInputs_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(null);
        when(mockLogicNode.getName()).thenReturn("TestMaxNode");

        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });
    }

    @Test
    @DisplayName("should validate successfully with valid inputs")
    void testValidateNode_WithValidInputs_ShouldNotThrow() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            operation.validateNode(mockLogicNode);
        });
    }

    @Test
    @DisplayName("should declare its return type as Double")
    void getReturnType_ShouldReturnDoubleClass() {
        assertEquals(Double.class, operation.getReturnType());
    }
}