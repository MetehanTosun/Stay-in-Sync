package predicates.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.general_predicates.NotExistsOperator;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.ProviderNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: NOT_EXISTS")
public class NotExistsOperatorTest {

    private NotExistsOperator operation;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LogicNode mockLogicNode;
    @Mock
    private ProviderNode mockInputNode1;
    @Mock
    private ProviderNode mockInputNode2;

    @BeforeEach
    void setUp() {
        operation = new NotExistsOperator();
    }

    @Test
    @DisplayName("should return true when the specified path does not exist")
    void testExecute_WhenPathDoesNotExist_ShouldReturnTrue() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockInputNode1));
        when(mockInputNode1.getJsonPath()).thenReturn("source.sensor.pressure");

        JsonNode dataContextNode = objectMapper.createObjectNode().set("source",
                objectMapper.createObjectNode().set("sensor",
                        objectMapper.createObjectNode().put("temperature", 25)
                )
        );
        Map<String, JsonNode> dataContext = Map.of("source", dataContextNode.get("source"));

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

        JsonNode dataContextNode = objectMapper.createObjectNode().set("source",
                objectMapper.createObjectNode().set("sensor",
                        objectMapper.createObjectNode().put("temperature", 25)
                )
        );
        Map<String, JsonNode> dataContext = Map.of("source", dataContextNode.get("source"));

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

        JsonNode dataContextNode = objectMapper.createObjectNode().set("source",
                objectMapper.createObjectNode().set("sensor",
                        objectMapper.createObjectNode().put("temperature", 25)
                )
        );
        Map<String, JsonNode> dataContext = Map.of("source", dataContextNode.get("source"));

        // ACT
        Object result = operation.execute(mockLogicNode, dataContext);

        // ASSERT
        assertFalse((Boolean) result, "Should return false as soon as one path is found.");
    }

    @Test
    @DisplayName("should declare its return type as Boolean")
    void getReturnType_ShouldReturnBooleanClass() {
        assertEquals(Boolean.class, operation.getReturnType());
    }
}