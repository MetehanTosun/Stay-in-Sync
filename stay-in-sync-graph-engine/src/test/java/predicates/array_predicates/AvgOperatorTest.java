package predicates.array_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.array_predicates.AvgOperator;
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
 * Unit tests for the AvgOperator based on multiple array/collection inputs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: AVG")
public class AvgOperatorTest {

    private AvgOperator operation;

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
        operation = new AvgOperator();
    }

    @Test
    @DisplayName("should return the correct average of numbers from multiple list/array inputs")
    void testExecute_WithMultipleCollectionInputs_ShouldReturnAverage() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));

        // Input 1: [2, 4] → avg contribution: (2+4)/total
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(2, 4));
        // Input 2: [6, 8] → avg contribution: (6+8)/total
        when(mockInputNode2.getCalculatedResult()).thenReturn(new Object[]{6, 8});
        // Input 3: [10] → avg contribution: 10/total
        when(mockInputNode3.getCalculatedResult()).thenReturn(List.of(10));

        // Total: (2+4+6+8+10) / 5 = 30 / 5 = 6.0

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(6.0, (Double) result);
        });
    }

    @Test
    @DisplayName("should return correct average from single array input")
    void testExecute_WithSingleArrayInput_ShouldReturnAverage() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(1, 2, 3, 4, 5)); // avg = 15/5 = 3.0

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(3.0, (Double) result);
        });
    }

    @Test
    @DisplayName("should handle different numeric types correctly")
    void testExecute_WithDifferentNumericTypes_ShouldReturnAverage() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(
                10,      // Integer
                5.0,     // Double
                2.5f,    // Float
                2L       // Long
        )); // avg = (10 + 5.0 + 2.5 + 2) / 4 = 19.5 / 4 = 4.875

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(4.875, (Double) result);
        });
    }

    @Test
    @DisplayName("should ignore non-numeric values and calculate average of numeric ones")
    void testExecute_WithMixedTypes_ShouldIgnoreNonNumeric() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(Arrays.asList(1, "text", 3, null)); // Only 1, 3 count
        when(mockInputNode2.getCalculatedResult()).thenReturn(List.of(2, 4)); // Both count
        // avg = (1 + 3 + 2 + 4) / 4 = 10 / 4 = 2.5

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(2.5, (Double) result);
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
    void testExecute_WithMixedInputs_ShouldAverageOnlyValidArrays() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(2, 4)); // Valid: contributes 2, 4
        when(mockInputNode2.getCalculatedResult()).thenReturn("not an array"); // Invalid: ignored
        when(mockInputNode3.getCalculatedResult()).thenReturn(List.of(6, 8)); // Valid: contributes 6, 8
        // avg = (2 + 4 + 6 + 8) / 4 = 20 / 4 = 5.0

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(5.0, (Double) result);
        });
    }

    @Test
    @DisplayName("should handle decimal results correctly")
    void testExecute_WithDecimalResult_ShouldReturnCorrectAverage() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(1, 2)); // avg = 3/2 = 1.5

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(1.5, (Double) result);
        });
    }

    @Test
    @DisplayName("should throw validation exception if there are no inputs")
    void testValidateNode_WithNoInputs_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of());
        when(mockLogicNode.getName()).thenReturn("TestAvgNode");

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
        when(mockLogicNode.getName()).thenReturn("TestAvgNode");

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