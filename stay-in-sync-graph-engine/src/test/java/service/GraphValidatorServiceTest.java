package service;

import de.unistuttgart.graphengine.exception.NodeConfigurationException;
import de.unistuttgart.graphengine.logic_operator.LogicOperator;
import de.unistuttgart.graphengine.nodes.*;
import de.unistuttgart.graphengine.service.GraphTopologicalSorter;
import de.unistuttgart.graphengine.service.GraphValidatorService;
import de.unistuttgart.graphengine.validation_error.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GraphValidatorServiceTest {

    @InjectMocks
    private GraphValidatorService validator;

    @Mock
    private GraphTopologicalSorter sorter;

    @BeforeEach
    void setUp() {
        GraphTopologicalSorter.SortResult noCycleResult = new GraphTopologicalSorter.SortResult(List.of(), false, List.of());
        lenient().when(sorter.sort(anyList())).thenReturn(noCycleResult);
    }

    @Test
    void testValidateGraph_WithValidDefaultGraph_ShouldReturnNoErrors() {
        // ARRANGE
        ConfigNode configNode = createConfigNode(0);
        FinalNode finalNode = createFinalNode(1, configNode);
        List<Node> graph = List.of(configNode, finalNode);

        // ACT
        List<ValidationError> errors = validator.validateGraph(graph, graph.size());

        // ASSERT
        assertTrue(errors.isEmpty(), "A valid default graph should produce no validation errors.");
    }

    @Test
    void testValidateGraph_WithMissingFinalNode_ShouldReturnError() {
        // ARRANGE
        ConfigNode configNode = createConfigNode(0);
        List<Node> graph = List.of(configNode);

        // ACT
        List<ValidationError> errors = validator.validateGraph(graph, graph.size());

        // ASSERT
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof FinalNodeError, "Should return a FinalNodeError.");
        assertTrue(errors.get(0).getMessage().contains("Exactly one FinalNode is required"), "Error message should indicate a missing FinalNode.");
    }

    @Test
    void testValidateGraph_WithMissingConfigNode_ShouldReturnError() {
        // ARRANGE
        FinalNode finalNode = new FinalNode();
        finalNode.setId(0);
        List<Node> graph = List.of(finalNode);

        // ACT
        List<ValidationError> errors = validator.validateGraph(graph, graph.size());

        // ASSERT
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof ConfigNodeError, "Should return a ConfigNodeError for a missing ConfigNode.");
    }

    @Test
    void testValidateGraph_WithCycle_ShouldReturnCycleError() {
        // ARRANGE
        ConfigNode configNode = createConfigNode(0);
        LogicNode nodeA;
        LogicNode nodeB;
        FinalNode finalNode;
        ConstantNode dummyInput;
        
        try {
            nodeA = new LogicNode("A", LogicOperator.AND);
            nodeA.setId(1);
            nodeB = new LogicNode("B", LogicOperator.AND);
            nodeB.setId(2);
            dummyInput = new ConstantNode("dummy", true);
            dummyInput.setId(4);
        } catch (NodeConfigurationException e) {
            fail("Unexpected NodeConfigurationException during test setup: " + e.getMessage());
            return;
        }
        
        finalNode = createFinalNode(3, nodeA);

        nodeA.setInputNodes(List.of(nodeB, dummyInput));
        nodeB.setInputNodes(List.of(nodeA, dummyInput)); // Cycle
        List<Node> graph = List.of(configNode, nodeA, nodeB, finalNode, dummyInput);

        // Mocking for a Graph with a cycle
        GraphTopologicalSorter.SortResult cycleResult = new GraphTopologicalSorter.SortResult(List.of(), true, List.of(1, 2));
        when(sorter.sort(graph)).thenReturn(cycleResult);

        // ACT
        List<ValidationError> errors = validator.validateGraph(graph, graph.size());

        // ASSERT
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof CycleError, "Should return a CycleError.");
    }

    @Test
    void testValidateGraph_WithWrongInputTypeForFinalNode_ShouldReturnError() {
        // ARRANGE
        ConfigNode configNode = createConfigNode(0);
        ConstantNode numberInput;
        try {
            numberInput = new ConstantNode("numberInput", 123);
            numberInput.setId(1);
        } catch (NodeConfigurationException e) {
            fail("Unexpected NodeConfigurationException during test setup: " + e.getMessage());
            return;
        }

        FinalNode finalNode = createFinalNode(2, numberInput);
        List<Node> graph = List.of(configNode, numberInput, finalNode);

        // ACT
        List<ValidationError> errors = validator.validateGraph(graph, graph.size());

        // ASSERT
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof FinalNodeError);
        assertTrue(errors.get(0).getMessage().contains("The FinalNode input must be of type BOOLEAN"));
    }

    @Test
    void testValidateGraph_WithNullGraph_ShouldReturnError() {
        // ACT
        List<ValidationError> errors = validator.validateGraph(null, 0);

        // ASSERT
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof FinalNodeError);
        assertEquals("Graph cannot be null or empty.", errors.get(0).getMessage());
    }

    @Test
    void testValidateGraph_WithEmptyGraph_ShouldReturnError() {
        // ACT
        List<ValidationError> errors = validator.validateGraph(Collections.emptyList(), 0);

        // ASSERT
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof FinalNodeError);
        assertEquals("Graph cannot be null or empty.", errors.get(0).getMessage());
    }

    @Test
    void testValidateGraph_WithMultipleFinalNodes_ShouldReturnError() {
        // ARRANGE
        ConfigNode configNode = createConfigNode(0);
        FinalNode finalNode1 = createFinalNode(1, configNode);
        FinalNode finalNode2 = createFinalNode(2, configNode);
        List<Node> graph = List.of(configNode, finalNode1, finalNode2);

        // ACT
        List<ValidationError> errors = validator.validateGraph(graph, graph.size());

        // ASSERT
        assertTrue(errors.stream().anyMatch(e -> e instanceof FinalNodeError && e.getMessage().contains("Exactly one FinalNode is required, but found 2")));
    }

    @Test
    void testValidateGraph_WithMultipleConfigNodes_ShouldReturnError() {
        // ARRANGE
        ConfigNode configNode1 = createConfigNode(0);
        ConfigNode configNode2 = createConfigNode(1);
        FinalNode finalNode = createFinalNode(2, configNode1);
        List<Node> graph = List.of(configNode1, configNode2, finalNode);

        // ACT
        List<ValidationError> errors = validator.validateGraph(graph, graph.size());

        // ASSERT
        assertTrue(errors.stream().anyMatch(e -> e instanceof ConfigNodeError && e.getMessage().contains("Exactly one ConfigNode is required, but found 2")));
    }

    @Test
    void testValidateGraph_WithFinalNodeHavingNullInputs_ShouldReturnError() {
        // ARRANGE
        ConfigNode configNode = createConfigNode(0);
        FinalNode finalNode = new FinalNode();
        finalNode.setId(1);
        finalNode.setInputNodes(null); // Explicitly set to null
        List<Node> graph = List.of(configNode, finalNode);

        // ACT
        List<ValidationError> errors = validator.validateGraph(graph, graph.size());

        // ASSERT
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof FinalNodeError);
        assertEquals("The FinalNode must have exactly one input connection.", errors.get(0).getMessage());
    }

    @Test
    void testValidateGraph_WithFinalNodeHavingNoInputs_ShouldReturnError() {
        // ARRANGE
        ConfigNode configNode = createConfigNode(0);
        FinalNode finalNode = new FinalNode();
        finalNode.setId(1);
        finalNode.setInputNodes(new ArrayList<>()); // Empty list
        List<Node> graph = List.of(configNode, finalNode);

        // ACT
        List<ValidationError> errors = validator.validateGraph(graph, graph.size());

        // ASSERT
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof FinalNodeError);
        assertEquals("The FinalNode must have exactly one input connection.", errors.get(0).getMessage());
    }

    @Test
    void testValidateGraph_WithFinalNodeHavingMultipleInputs_ShouldReturnError() {
        // ARRANGE
        ConfigNode configNode = createConfigNode(0);
        ConstantNode input1;
        ConstantNode input2;
        try {
            input1 = new ConstantNode("in1", true);
            input1.setId(1);
            input2 = new ConstantNode("in2", true);
            input2.setId(2);
        } catch (NodeConfigurationException e) {
            fail("Unexpected NodeConfigurationException during test setup: " + e.getMessage());
            return;
        }

        FinalNode finalNode = new FinalNode();
        finalNode.setId(3);
        finalNode.setInputNodes(List.of(input1, input2));
        List<Node> graph = List.of(configNode, finalNode, input1, input2);

        // ACT
        List<ValidationError> errors = validator.validateGraph(graph, graph.size());

        // ASSERT
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof FinalNodeError);
        assertEquals("The FinalNode must have exactly one input connection.", errors.get(0).getMessage());
    }

    @Test
    void testValidateGraph_WithInvalidOperatorConfiguration_ShouldReturnError() {
        // ARRANGE
        ConfigNode configNode = createConfigNode(0);
        ConstantNode const1;
        LogicNode invalidAddNode;
        
        try {
            const1 = new ConstantNode("val1", 10);
            const1.setId(1);
            invalidAddNode = new LogicNode("Invalid Add", LogicOperator.ADD);
            invalidAddNode.setId(2);
            invalidAddNode.setInputNodes(List.of(const1));
        } catch (NodeConfigurationException e) {
            fail("Unexpected NodeConfigurationException during test setup: " + e.getMessage());
            return;
        }

        FinalNode finalNode = createFinalNode(3, configNode);
        List<Node> graph = List.of(configNode, invalidAddNode, finalNode, const1);

        // ACT
        List<ValidationError> errors = validator.validateGraph(graph, graph.size());

        // ASSERT
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof OperatorConfigurationError);
        OperatorConfigurationError opError = (OperatorConfigurationError) errors.get(0);
        assertEquals(2, opError.getNodeId());
        assertTrue(opError.getMessage().contains("requires at least 2 inputs"));
    }

    @Test
    void testValidateGraph_WithMultipleDifferentErrors_ShouldCollectAllErrors() {
        // ARRANGE
        FinalNode final1 = new FinalNode();
        final1.setId(1);
        FinalNode final2 = new FinalNode();
        final2.setId(2);
        List<Node> graph = List.of(final1, final2);

        // ACT
        List<ValidationError> errors = validator.validateGraph(graph, graph.size());

        // ASSERT
        assertEquals(2, errors.size(), "Should find both a ConfigNodeError and a FinalNodeError.");
        assertTrue(errors.stream().anyMatch(e -> e instanceof ConfigNodeError));
        assertTrue(errors.stream().anyMatch(e -> e instanceof FinalNodeError && e.getMessage().contains("found 2")));
    }

    // region Helper Methods

    private ConfigNode createConfigNode(int id) {
        ConfigNode node = new ConfigNode();
        node.setId(id);
        return node;
    }

    private FinalNode createFinalNode(int id, Node input) {
        FinalNode node = new FinalNode();
        node.setId(id);
        node.setInputNodes(List.of(input));
        return node;
    }
}