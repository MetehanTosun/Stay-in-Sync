package predicates.array_predicates;

import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.array_predicates.SumOperator;
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
 * Unit tests for the SumOperator based on multiple array/collection inputs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: SUM")
public class SumOperatorTest {

    private SumOperator operation;

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
        operation = new SumOperator();
    }

    @Test
    @DisplayName("should return the correct sum of numbers from multiple list/array inputs")
    void testExecute_WithMultipleCollectionInputs_ShouldReturnSum() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));

        // Input 1: Eine Liste von Zahlen
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(10.5, 5));
        // Input 2: Ein Array von Zahlen
        when(mockInputNode2.getCalculatedResult()).thenReturn(new Object[]{2.5, 2.0});
        // Input 3: Eine Liste mit gemischten Typen (sollte die Strings ignorieren)
        when(mockInputNode3.getCalculatedResult()).thenReturn(List.of(10, "not_a_number"));

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(30.0, (Double) result);
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
        when(mockInputNode1.getCalculatedResult()).thenReturn(Arrays.asList("text", true, null)); // ← Arrays.asList statt List.of

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(0.0, (Double) result);
        });
    }

    @Test
    @DisplayName("should handle different numeric types correctly")
    void testExecute_WithDifferentNumericTypes_ShouldReturnSum() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(
                10,      // Integer
                5.5,     // Double
                2.5f,    // Float
                3L       // Long
        ));

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(21.0, (Double) result);
        });
    }

    @Test
    @DisplayName("should handle single array input correctly")
    void testExecute_WithSingleArrayInput_ShouldReturnSum() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(1, 2, 3, 4, 5)); // ← Vollständiges Stubbing

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(15.0, (Double) result);
        });
    }

    @Test
    @DisplayName("should handle mixed valid and invalid inputs")
    void testExecute_WithMixedInputs_ShouldSumOnlyValidArrays() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2, mockInputNode3));
        when(mockInputNode1.getCalculatedResult()).thenReturn(List.of(1, 2)); // Valid array
        when(mockInputNode2.getCalculatedResult()).thenReturn("not an array"); // Invalid
        when(mockInputNode3.getCalculatedResult()).thenReturn(List.of(3, 4)); // Valid array

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertEquals(10.0, (Double) result); // Only sums from valid arrays
        });
    }

    @Test
    @DisplayName("should throw validation exception if there are no inputs")
    void testValidateNode_WithNoInputs_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of());
        when(mockLogicNode.getName()).thenReturn("TestSumNode");

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
        when(mockLogicNode.getName()).thenReturn("TestSumNode");

        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });
    }

    @Test
    @DisplayName("should validate successfully with single valid input")
    void testValidateNode_WithSingleInput_ShouldNotThrow() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            operation.validateNode(mockLogicNode);
        });
    }

    @Test
    @DisplayName("should validate successfully with multiple valid inputs")
    void testValidateNode_WithMultipleInputs_ShouldNotThrow() {
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