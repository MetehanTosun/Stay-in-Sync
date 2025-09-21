package predicates.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.general_predicates.ExistsOperator;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.graphengine.nodes.ProviderNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the ExistsOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: EXISTS")
public class ExistsOperatorTest {

    private ExistsOperator operation;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, JsonNode> dataContext;

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private ProviderNode mockInputNode1;
    @Mock
    private ProviderNode mockInputNode2;
    @Mock
    private Node mockInvalidInputNode;

    @BeforeEach
    void setUp() {
        operation = new ExistsOperator();
        // Setup a default data context for happy path tests
        JsonNode dataContextNode = objectMapper.createObjectNode()
                .set("source", objectMapper.createObjectNode()
                        .set("sensor", objectMapper.createObjectNode()
                                .put("temperature", 25)
                                .putNull("pressure") // path exists, but value is null
                                .put("humidity", 50)));
        dataContext = Map.of("source", dataContextNode.get("source"));
    }

    @Test
    @DisplayName("should return true when the specified path exists")
    void testExecute_WhenPathExists_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.temperature");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return true when path exists but its value is null")
    void testExecute_WhenPathExistsWithNullValue_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.pressure");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when the specified path does not exist")
    void testExecute_WhenPathDoesNotExist_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.nonexistent"); // This path is missing

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return true only if ALL specified paths exist")
    void testExecute_WhenMultiplePathsExist_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.temperature");
        when(mockInputNode2.getJsonPath()).thenReturn("source.sensor.humidity");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("should return false when dataContext is null")
    void testExecute_WhenDataContextIsNull_ShouldReturnFalse() {
        // KORREKTUR: Die when(...) Anweisungen wurden entfernt, weil die Methode sofort aussteigt
        // und die Mocks nie verwendet werden.
        assertFalse((Boolean) operation.execute(mockLogicNode, null));
    }

    @Test
    @DisplayName("should return false when dataContext is empty")
    void testExecute_WhenDataContextIsEmpty_ShouldReturnFalse() {
        // KORREKTUR: Die when(...) Anweisungen wurden entfernt.
        assertFalse((Boolean) operation.execute(mockLogicNode, Collections.emptyMap()));
    }

    @Test
    @DisplayName("should return false when source key does not exist in dataContext")
    void testExecute_WhenSourceKeyIsMissing_ShouldReturnFalse() {
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("wrong_source.sensor.temperature");
        assertFalse((Boolean) operation.execute(mockLogicNode, dataContext));
    }

    @Test
    @DisplayName("should return true when path is just the source key")
    void testExecute_WhenPathIsJustSourceKey_ShouldReturnTrue() {
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source");
        assertTrue((Boolean) operation.execute(mockLogicNode, dataContext));
    }

    // ==================== NODE VALIDATION TESTS ====================

    @Test
    @DisplayName("should throw exception when inputs list is null")
    void testValidateNode_WhenInputsListIsNull_ShouldThrowException() {
        when(mockLogicNode.getInputNodes()).thenReturn(null);
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }



    @Test
    @DisplayName("should throw exception when inputs list is empty")
    void testValidateNode_WhenInputsListIsEmpty_ShouldThrowException() {
        when(mockLogicNode.getInputNodes()).thenReturn(Collections.emptyList());
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }

    @Test
    @DisplayName("should throw exception when an input is not a ProviderNode")
    void testValidateNode_WhenInputIsNotProviderNode_ShouldThrowException() {
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInvalidInputNode));
        assertThrows(OperatorValidationException.class, () -> operation.validateNode(mockLogicNode));
    }


    @Test
    @DisplayName("should declare its return type as Boolean")
    void getReturnType_ShouldReturnBooleanClass() {
        assertEquals(Boolean.class, operation.getReturnType());
    }
}