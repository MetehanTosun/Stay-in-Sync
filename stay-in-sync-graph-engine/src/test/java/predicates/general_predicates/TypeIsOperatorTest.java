package predicates.general_predicates;

import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.general_predicates.TypeIsOperator;
import de.unistuttgart.graphengine.nodes.ConstantNode;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the TypeIsOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: TYPE_IS")
public class TypeIsOperatorTest {

    private TypeIsOperator operation;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private ConstantNode mockTypeConstantNode;
    @Mock
    private Node mockValueInput;
    @Mock
    private Node mockValueInput2;
    @Mock
    private Node mockNonConstantNode;

    @BeforeEach
    void setUp() {
        operation = new TypeIsOperator();
    }

    // ==================== EXECUTE TESTS ====================

    static Stream<Arguments> typeProvider() {
        return Stream.of(
                Arguments.of("string", "a string value"),
                Arguments.of("number", 123),
                Arguments.of("number", 123.45),
                Arguments.of("boolean", true),
                Arguments.of("map", new HashMap<>()),
                Arguments.of("collection", new ArrayList<>()),
                Arguments.of("collection", new HashSet<>()),
                Arguments.of("collection", new Object[]{})
        );
    }

    @ParameterizedTest
    @MethodSource("typeProvider")
    @DisplayName("should return true when the value's type matches the string")
    void testExecute_WhenTypeMatches_ShouldReturnTrue(String typeString, Object value) {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockTypeConstantNode, mockValueInput));
        when(mockTypeConstantNode.getCalculatedResult()).thenReturn(typeString);
        when(mockValueInput.getCalculatedResult()).thenReturn(value);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when the value's type does not match")
    void testExecute_WhenTypeDoesNotMatch_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockTypeConstantNode, mockValueInput));
        when(mockTypeConstantNode.getCalculatedResult()).thenReturn("boolean");
        when(mockValueInput.getCalculatedResult()).thenReturn("true"); // Ist ein String, kein Boolean

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should handle type string case-insensitively")
    void testExecute_WhenTypeStringIsMixedCase_ShouldWork() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockTypeConstantNode, mockValueInput));
        when(mockTypeConstantNode.getCalculatedResult()).thenReturn("NuMbEr");
        when(mockValueInput.getCalculatedResult()).thenReturn(123);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true only if ALL values match the type")
    void testExecute_WhenAllMultipleValuesMatch_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockTypeConstantNode, mockValueInput, mockValueInput2));
        when(mockTypeConstantNode.getCalculatedResult()).thenReturn("number");
        when(mockValueInput.getCalculatedResult()).thenReturn(10);
        when(mockValueInput2.getCalculatedResult()).thenReturn(20.5);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if ANY value does not match the type")
    void testExecute_WhenOneOfMultipleValuesDoesNotMatch_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockTypeConstantNode, mockValueInput, mockValueInput2));
        when(mockTypeConstantNode.getCalculatedResult()).thenReturn("number");
        when(mockValueInput.getCalculatedResult()).thenReturn(10);
        when(mockValueInput2.getCalculatedResult()).thenReturn("not a number");

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @DisplayName("should throw exception if input count is less than 2")
    void testValidateNode_WithLessThanTwoInputs_ShouldThrowException() {
        assertThrows(OperatorValidationException.class, () -> {
            when(mockLogicNode.getInputNodes()).thenReturn(null);
            operation.validateNode(mockLogicNode);
        });
        assertThrows(OperatorValidationException.class, () -> {
            when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockTypeConstantNode));
            operation.validateNode(mockLogicNode);
        });
    }

    @Test
    @DisplayName("should throw exception if first input is not a ConstantNode")
    void testValidateNode_WhenFirstInputIsNotConstantNode_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockNonConstantNode, mockValueInput));
        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should throw exception if ConstantNode value is not a string")
    void testValidateNode_WhenConstantValueIsNotString_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockTypeConstantNode, mockValueInput));
        when(mockTypeConstantNode.getValue()).thenReturn(123); // Integer statt String
        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should throw exception if ConstantNode value is invalid type string")
    void testValidateNode_WhenTypeStringIsInvalid_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockTypeConstantNode, mockValueInput));
        when(mockTypeConstantNode.getValue()).thenReturn("integer"); // Ungültiger Typ
        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should declare its return type as Boolean")
    void getReturnType_ShouldReturnBooleanClass() {
        assertEquals(Boolean.class, operation.getReturnType());
    }
}