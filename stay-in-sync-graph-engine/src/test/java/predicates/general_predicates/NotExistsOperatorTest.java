package predicates.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.general_predicates.NotExistsOperator;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: NOT_EXISTS")
public class NotExistsOperatorTest {

    private NotExistsOperator operation;
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
        operation = new NotExistsOperator();
        JsonNode dataContextNode = objectMapper.createObjectNode().set("source",
                objectMapper.createObjectNode().set("sensor",
                        objectMapper.createObjectNode().put("temperature", 25)
                )
        );
        dataContext = Map.of("source", dataContextNode.get("source"));
    }

    @Test
    @DisplayName("should return true when the specified path does not exist")
    void testExecute_WhenPathDoesNotExist_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.pressure");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }

    @Test
    @DisplayName("should return false when the specified path exists")
    void testExecute_WhenPathExists_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.temperature");

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertFalse((Boolean) result);
    }

    @Test
    @DisplayName("should return false if any of multiple paths exist")
    void testExecute_WhenOneOfMultiplePathsExists_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.pressure"); // existiert nicht
        when(mockInputNode2.getJsonPath()).thenReturn("source.sensor.temperature"); // existiert

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertFalse((Boolean) result, "Should return false as soon as one path is found.");
    }

    @Test
    @DisplayName("should return true when ALL of multiple paths do not exist")
    void testExecute_WhenAllMultiplePathsDoNotExist_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1, mockInputNode2));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.pressure"); // existiert nicht
        when(mockInputNode2.getJsonPath()).thenReturn("source.sensor.humidity"); // existiert nicht

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertTrue((Boolean) result);
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("should return true when dataContext is null")
    void testExecute_WhenDataContextIsNull_ShouldReturnTrue() {
        // KORREKTUR: Die Mocks wurden entfernt, da die Methode sofort aussteigt.
        assertTrue((Boolean) operation.execute(mockLogicNode, null));
    }

    @Test
    @DisplayName("should return true when dataContext is empty")
    void testExecute_WhenDataContextIsEmpty_ShouldReturnTrue() {
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.temperature");
        assertTrue((Boolean) operation.execute(mockLogicNode, Collections.emptyMap()));
    }

    @Test
    @DisplayName("should return true when source key does not exist in dataContext")
    void testExecute_WhenSourceKeyIsMissing_ShouldReturnTrue() {
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("wrong_source.sensor.temperature");
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