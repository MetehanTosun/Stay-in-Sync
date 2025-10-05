package predicates.number_predicates;


import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.number_predicates.GreaterThanOperator;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the GreaterThanOperation.
 * This test verifies the logic of the GREATER_THAN operator in isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: GREATER_THAN")
public class GreaterThanOperationTest {

    // Die ECHTE Klasse, die wir testen wollen
    private GreaterThanOperator operation;

    // Simulationen (Mocks) für die Abhängigkeiten
    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private Node mockInputNode1;
    @Mock
    private Node mockInputNode2;

    @BeforeEach
    void setUp() {
        operation = new GreaterThanOperator();
    }

    @Test
    @DisplayName("should return true if first value is greater than second")
    void testExecute_WhenFirstIsGreater_ShouldReturnTrue() {
        // ARRANGE: Simuliere die Ergebnisse der Input-Knoten
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(10.0);
        when(mockInputNode2.getCalculatedResult()).thenReturn(5.0);

        // ACT: Führe die execute-Methode der echten Operation-Klasse aus
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT: Prüfe das Ergebnis
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false if first value is less than second")
    void testExecute_WhenFirstIsLess_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(5.0);
        when(mockInputNode2.getCalculatedResult()).thenReturn(10.0);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false if values are equal")
    void testExecute_WhenValuesAreEqual_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getCalculatedResult()).thenReturn(10.0);
        when(mockInputNode2.getCalculatedResult()).thenReturn(10.0);

        // ACT
        Object result = operation.execute(mockLogicNode, null);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 2")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE: Simuliere einen LogicNode mit nur einem Input
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));

        // ACT & ASSERT: Prüfe, ob die validateNode-Methode wie erwartet einen Fehler wirft
        assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });
    }

    @Test
    @DisplayName("should declare its return type as Boolean")
    void getReturnType_ShouldReturnBooleanClass() {
        // ASSERT: Prüfe, ob der deklarierte Rückgabetyp korrekt ist
        assertEquals(Boolean.class, operation.getReturnType());
    }
}
