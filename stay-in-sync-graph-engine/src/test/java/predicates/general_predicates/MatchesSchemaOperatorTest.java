package predicates.general_predicates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.exception.OperatorValidationException;
import de.unistuttgart.graphengine.logic_operator.general_predicates.MatchesSchemaOperator;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import de.unistuttgart.graphengine.service.SchemaCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the MatchesSchemaOperator.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operation: MATCHES_SCHEMA")
public class MatchesSchemaOperatorTest {

    @InjectMocks
    private MatchesSchemaOperator operation;

    @Mock
    private SchemaCache schemaCache;

    @Mock
    private LogicNode mockLogicNode;

    @Mock
    private Node mockJsonInput;

    @Mock
    private Node mockSchemaInput;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Additional setup if needed
    }

    @Test
    @DisplayName("should return true when the JSON object matches the schema")
    void testExecute_WhenJsonMatchesSchema_ShouldReturnTrue() throws Exception {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockJsonInput, mockSchemaInput));

        JsonNode validJson = objectMapper.readTree("{\"temperature\": 25, \"status\": \"OK\"}");
        String schemaString = """
            {
              "type": "object",
              "properties": {
                "temperature": {
                  "type": "number"
                },
                "status": {
                  "type": "string"
                }
              },
              "required": ["temperature", "status"]
            }
            """;

        when(mockJsonInput.getCalculatedResult()).thenReturn(validJson);
        when(mockSchemaInput.getCalculatedResult()).thenReturn(schemaString);

        // Mock the schema cache to return a compiled schema
        JsonSchema mockCompiledSchema = mock(JsonSchema.class);
        when(schemaCache.getCompiledSchema(schemaString)).thenReturn(mockCompiledSchema);
        when(mockCompiledSchema.validate(validJson)).thenReturn(java.util.Set.of()); // No validation errors

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertTrue((Boolean) result);
        });

        verify(schemaCache).getCompiledSchema(schemaString);
        verify(mockCompiledSchema).validate(validJson);
    }

    @Test
    @DisplayName("should return false when the JSON object does not match the schema")
    void testExecute_WhenJsonDoesNotMatchSchema_ShouldReturnFalse() throws Exception {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockJsonInput, mockSchemaInput));

        JsonNode invalidJson = objectMapper.readTree("{\"temperature\": \"wrong_type\", \"status\": \"OK\"}");
        String schemaString = """
            {
              "type": "object",
              "properties": {
                "temperature": {
                  "type": "number"
                },
                "status": {
                  "type": "string"
                }
              },
              "required": ["temperature", "status"]
            }
            """;

        when(mockJsonInput.getCalculatedResult()).thenReturn(invalidJson);
        when(mockSchemaInput.getCalculatedResult()).thenReturn(schemaString);

        // Mock schema cache and validation errors
        JsonSchema mockCompiledSchema = mock(JsonSchema.class);
        when(schemaCache.getCompiledSchema(schemaString)).thenReturn(mockCompiledSchema);
        when(mockCompiledSchema.validate(invalidJson)).thenReturn(java.util.Set.of(
                mock(com.networknt.schema.ValidationMessage.class) // Mock validation error
        ));

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertFalse((Boolean) result);
        });
    }

    @Test
    @DisplayName("should return false when first input is not JsonNode")
    void testExecute_WhenFirstInputIsNotJsonNode_ShouldReturnFalse() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockJsonInput, mockSchemaInput));
        when(mockJsonInput.getCalculatedResult()).thenReturn("not a JsonNode");
        when(mockSchemaInput.getCalculatedResult()).thenReturn("{}");

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertFalse((Boolean) result);
        });

        // Schema cache should not be called if input validation fails
        verifyNoInteractions(schemaCache);
    }

    @Test
    @DisplayName("should return false when second input is not String")
    void testExecute_WhenSecondInputIsNotString_ShouldReturnFalse() throws Exception {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockJsonInput, mockSchemaInput));

        JsonNode validJson = objectMapper.readTree("{\"test\": \"value\"}");
        when(mockJsonInput.getCalculatedResult()).thenReturn(validJson);
        when(mockSchemaInput.getCalculatedResult()).thenReturn(123); // Not a string

        // ACT & ASSERT
        assertDoesNotThrow(() -> {
            Object result = operation.execute(mockLogicNode, null);
            assertFalse((Boolean) result);
        });

        verifyNoInteractions(schemaCache);
    }

    @Test
    @DisplayName("should throw GraphEvaluationException when schema cache throws exception")
    void testExecute_WhenSchemaCacheThrowsException_ShouldThrowGraphEvaluationException() throws Exception {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockJsonInput, mockSchemaInput));
        when(mockLogicNode.getName()).thenReturn("TestNode");

        JsonNode validJson = objectMapper.readTree("{\"test\": \"value\"}");
        String invalidSchemaString = "invalid schema";

        when(mockJsonInput.getCalculatedResult()).thenReturn(validJson);
        when(mockSchemaInput.getCalculatedResult()).thenReturn(invalidSchemaString);
        when(schemaCache.getCompiledSchema(invalidSchemaString)).thenThrow(new RuntimeException("Invalid schema"));

        // ACT & ASSERT
        assertThrows(GraphEvaluationException.class, () -> {
            operation.execute(mockLogicNode, null);
        });
    }

    @Test
    @DisplayName("should throw exception if input count is not exactly 2")
    void testValidateNode_WithIncorrectInputCount_ShouldThrowException() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockJsonInput));
        when(mockLogicNode.getName()).thenReturn("TestNode");

        // ACT & ASSERT
        assertThrows(OperatorValidationException.class, () -> {
            operation.validateNode(mockLogicNode);
        });
    }

    @Test
    @DisplayName("should validate successfully with exactly 2 inputs")
    void testValidateNode_WithCorrectInputCount_ShouldNotThrow() {
        // ARRANGE
        when(mockLogicNode.getInputNodes()).thenReturn(List.of(mockJsonInput, mockSchemaInput));

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