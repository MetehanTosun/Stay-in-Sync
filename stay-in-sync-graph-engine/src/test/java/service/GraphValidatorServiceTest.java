package service;

import de.unistuttgart.graphengine.exception.NodeConfigurationException;
import de.unistuttgart.graphengine.logic_operator.LogicOperator;
import de.unistuttgart.graphengine.nodes.*;
import de.unistuttgart.graphengine.service.GraphValidatorService;
import de.unistuttgart.graphengine.validation_error.ConfigNodeError;
import de.unistuttgart.graphengine.validation_error.CycleError;
import de.unistuttgart.graphengine.validation_error.FinalNodeError;
import de.unistuttgart.graphengine.validation_error.ValidationError;
import de.unistuttgart.graphengine.util.GraphTopologicalSorter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
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
        when(sorter.sort(anyList())).thenReturn(noCycleResult);
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
        // Ein Graph nur mit einer FinalNode, dem aber der obligatorische ConfigNode fehlt.
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
    void testValidateGraph_WithCycle_ShouldReturnCycleError() throws NodeConfigurationException {
        // ARRANGE
        ConfigNode configNode = createConfigNode(0);
        LogicNode nodeA = new LogicNode("A", LogicOperator.AND);
        nodeA.setId(1);
        LogicNode nodeB = new LogicNode("B", LogicOperator.AND);
        nodeB.setId(2);
        FinalNode finalNode = createFinalNode(3, nodeA);
        ConstantNode dummyInput = new ConstantNode("dummy", true);
        dummyInput.setId(4);

        nodeA.setInputNodes(List.of(nodeB, dummyInput));
        nodeB.setInputNodes(List.of(nodeA, dummyInput)); // Cycle
        List<Node> graph = List.of(configNode, nodeA, nodeB, finalNode, dummyInput);

        GraphTopologicalSorter.SortResult cycleResult = new GraphTopologicalSorter.SortResult(List.of(), true, List.of(1, 2));
        when(sorter.sort(graph)).thenReturn(cycleResult);

        // ACT
        List<ValidationError> errors = validator.validateGraph(graph, graph.size());

        // ASSERT
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof CycleError, "Should return a CycleError.");
    }

    @Test
    void testValidateGraph_WithWrongInputTypeForFinalNode_ShouldReturnError() throws NodeConfigurationException {
        // ARRANGE
        ConfigNode configNode = createConfigNode(0);
        ConstantNode const1 = new ConstantNode("val1", 10);
        const1.setId(1);
        ConstantNode const2 = new ConstantNode("val2", 20);
        const2.setId(2);

        LogicNode numberNode = new LogicNode("A", LogicOperator.ADD);
        numberNode.setId(3);
        numberNode.setInputNodes(List.of(const1, const2));

        FinalNode finalNode = new FinalNode();
        finalNode.setId(4);
        finalNode.setInputNodes(List.of(numberNode));
        List<Node> graph = List.of(configNode, numberNode, finalNode, const1, const2);

        // ACT
        List<ValidationError> errors = validator.validateGraph(graph, graph.size());

        // ASSERT
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof FinalNodeError, "Should return a FinalNodeError for wrong input type.");
    }

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