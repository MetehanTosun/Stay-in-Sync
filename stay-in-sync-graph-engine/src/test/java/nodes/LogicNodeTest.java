package nodes;

import com.fasterxml.jackson.databind.JsonNode;
import de.unistuttgart.graphengine.exception.GraphEvaluationException;
import de.unistuttgart.graphengine.exception.NodeConfigurationException;
import de.unistuttgart.graphengine.logic_operator.LogicOperator;
import de.unistuttgart.graphengine.logic_operator.Operation;
import de.unistuttgart.graphengine.nodes.LogicNode;
import de.unistuttgart.graphengine.nodes.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LogicNode Tests")
public class LogicNodeTest {

    @Mock
    private LogicOperator mockOperator;
    @Mock
    private Operation mockOperation;
    @Mock
    private Node mockInputNode1;
    @Mock
    private Node mockInputNode2;

    private Map<String, JsonNode> dataContext;

    @BeforeEach
    void setUp() {
        dataContext = new HashMap<>();
        when(mockOperator.getOperationStrategy()).thenReturn(mockOperation);
    }

    // ===== CONSTRUCTOR VALIDATION TESTS =====

    @Test
    @DisplayName("should create LogicNode with valid parameters")
    void testConstructor_WithValidParameters_ShouldSucceed() throws Exception {
        // ACT
        LogicNode node = new LogicNode("TestNode", mockOperator);

        // ASSERT
        assertEquals("TestNode", node.getName());
        assertEquals(mockOperator, node.getOperator());
    }

    @Test
    @DisplayName("should throw exception for null name")
    void testConstructor_WithNullName_ShouldThrowException() {
        // ACT & ASSERT
        NodeConfigurationException exception = assertThrows(NodeConfigurationException.class, () -> {
            new LogicNode(null, mockOperator);
        });

        assertTrue(exception.getMessage().contains("Name for LogicNode cannot be null or empty"));
    }

    @Test
    @DisplayName("should throw exception for empty name")
    void testConstructor_WithEmptyName_ShouldThrowException() {
        // ACT & ASSERT
        NodeConfigurationException exception = assertThrows(NodeConfigurationException.class, () -> {
            new LogicNode("", mockOperator);
        });

        assertTrue(exception.getMessage().contains("Name for LogicNode cannot be null or empty"));
    }

    @Test
    @DisplayName("should throw exception for whitespace-only name")
    void testConstructor_WithWhitespaceName_ShouldThrowException() {
        // ACT & ASSERT
        NodeConfigurationException exception = assertThrows(NodeConfigurationException.class, () -> {
            new LogicNode("   \t\n   ", mockOperator);
        });

        assertTrue(exception.getMessage().contains("Name for LogicNode cannot be null or empty"));
    }

    @Test
    @DisplayName("should throw exception for null operator")
    void testConstructor_WithNullOperator_ShouldThrowException() {
        // ACT & ASSERT
        NodeConfigurationException exception = assertThrows(NodeConfigurationException.class, () -> {
            new LogicNode("TestNode", null);
        });

        assertTrue(exception.getMessage().contains("Operator for LogicNode cannot be null"));
    }

    @Test
    @DisplayName("should create LogicNode with inputs using varargs constructor")
    void testConstructor_WithInputs_ShouldSetInputNodes() throws Exception {
        // ACT
        LogicNode node = new LogicNode("TestNode", mockOperator, mockInputNode1, mockInputNode2);

        // ASSERT
        assertEquals("TestNode", node.getName());
        assertEquals(mockOperator, node.getOperator());
        assertEquals(Arrays.asList(mockInputNode1, mockInputNode2), node.getInputNodes());
    }

    // ===== CALCULATE METHOD TESTS (Strategy Pattern) =====

    @Test
    @DisplayName("should execute operation strategy successfully")
    void testCalculate_WithValidOperation_ShouldExecuteStrategy() throws Exception {
        // ARRANGE
        LogicNode node = new LogicNode("TestNode", mockOperator);
        Object expectedResult = "test_result";
        when(mockOperation.execute(node, dataContext)).thenReturn(expectedResult);

        // ACT
        node.calculate(dataContext);

        // ASSERT
        assertEquals(expectedResult, node.getCalculatedResult());
        verify(mockOperation).execute(node, dataContext);
    }

    @Test
    @DisplayName("should propagate GraphEvaluationException from operation")
    void testCalculate_WhenOperationThrowsGraphEvaluationException_ShouldPropagate() throws Exception {
        // ARRANGE
        LogicNode node = new LogicNode("TestNode", mockOperator);
        GraphEvaluationException originalException = new GraphEvaluationException(
                GraphEvaluationException.ErrorType.TYPE_MISMATCH,
                "Test Error",
                "Test message",
                null
        );
        when(mockOperation.execute(node, dataContext)).thenThrow(originalException);

        // ACT & ASSERT
        GraphEvaluationException thrown = assertThrows(GraphEvaluationException.class, () -> {
            node.calculate(dataContext);
        });

        assertEquals(originalException, thrown);
    }

    @Test
    @DisplayName("should wrap generic exceptions in GraphEvaluationException")
    void testCalculate_WhenOperationThrowsGenericException_ShouldWrapException() throws Exception {
        // ARRANGE
        LogicNode node = new LogicNode("TestNode", mockOperator);
        RuntimeException originalException = new RuntimeException("Generic error");
        when(mockOperation.execute(node, dataContext)).thenThrow(originalException);

        // ACT & ASSERT
        GraphEvaluationException thrown = assertThrows(GraphEvaluationException.class, () -> {
            node.calculate(dataContext);
        });

        assertEquals(GraphEvaluationException.ErrorType.EXECUTION_FAILED, thrown.getErrorType());
        assertTrue(thrown.getMessage().contains("TestNode"));
        assertTrue(thrown.getMessage().contains("An unexpected error occurred"));
        assertEquals(originalException, thrown.getCause());
    }

    @Test
    @DisplayName("should include operator and node name in wrapped exception message")
    void testCalculate_WhenWrappingException_ShouldIncludeDetails() throws Exception {
        // ARRANGE
        LogicNode node = new LogicNode("MySpecialNode", mockOperator);
        when(mockOperator.toString()).thenReturn("ADD_OPERATOR");
        when(mockOperation.execute(node, dataContext)).thenThrow(new RuntimeException("Test error"));

        // ACT & ASSERT
        GraphEvaluationException thrown = assertThrows(GraphEvaluationException.class, () -> {
            node.calculate(dataContext);
        });

        assertTrue(thrown.getMessage().contains("MySpecialNode"));
        assertTrue(thrown.getMessage().contains("ADD_OPERATOR"));
    }

    // ===== OUTPUT TYPE TESTS =====

    @Test
    @DisplayName("should return operation strategy return type")
    void testGetOutputType_WithValidOperator_ShouldReturnStrategyType() throws Exception {
        // ARRANGE
        LogicNode node = new LogicNode("TestNode", mockOperator);
        doReturn(Boolean.class).when(mockOperation).getReturnType();

        // ACT
        Class<?> outputType = node.getOutputType();

        // ASSERT
        assertEquals(Boolean.class, outputType);
    }

    @Test
    @DisplayName("should return Object.class when operator is null")
    void testGetOutputType_WithNullOperator_ShouldReturnObjectClass() throws Exception {
        // ARRANGE
        LogicNode node = new LogicNode("TestNode", mockOperator);
        node.setOperator(null); // Manually set to null after construction

        // ACT
        Class<?> outputType = node.getOutputType();

        // ASSERT
        assertEquals(Object.class, outputType);
    }

    @Test
    @DisplayName("should return strategy return type for different types")
    void testGetOutputType_WithDifferentReturnTypes_ShouldWork() throws Exception {
        // ARRANGE
        LogicNode node = new LogicNode("TestNode", mockOperator);

        // Test Integer return type
        doReturn(Integer.class).when(mockOperation).getReturnType();
        assertEquals(Integer.class, node.getOutputType());

        // Test String return type
        doReturn(String.class).when(mockOperation).getReturnType();
        assertEquals(String.class, node.getOutputType());
    }

    // ===== INTEGRATION TESTS =====

    @Test
    @DisplayName("should handle complete operation execution flow")
    void testCalculate_CompleteFlow_ShouldWork() throws Exception {
        // ARRANGE
        LogicNode node = new LogicNode("CompleteTestNode", mockOperator, mockInputNode1, mockInputNode2);

        // Setup input node results
        when(mockInputNode1.getCalculatedResult()).thenReturn(5);
        when(mockInputNode2.getCalculatedResult()).thenReturn(3);
        when(mockOperation.execute(node, dataContext)).thenReturn(8); // Simulated ADD operation
        doReturn(Integer.class).when(mockOperation).getReturnType();

        // ACT
        node.calculate(dataContext);

        // ASSERT
        assertEquals(8, node.getCalculatedResult());
        assertEquals(Integer.class, node.getOutputType());
        assertEquals(Arrays.asList(mockInputNode1, mockInputNode2), node.getInputNodes());
        verify(mockOperation).execute(node, dataContext);
    }

    @Test
    @DisplayName("should work with null dataContext")
    void testCalculate_WithNullDataContext_ShouldWork() throws Exception {
        // ARRANGE
        LogicNode node = new LogicNode("TestNode", mockOperator);
        when(mockOperation.execute(node, null)).thenReturn("result");

        // ACT
        node.calculate(null);

        // ASSERT
        assertEquals("result", node.getCalculatedResult());
        verify(mockOperation).execute(node, null);
    }
}
