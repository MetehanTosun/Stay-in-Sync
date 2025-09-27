package service;

import de.unistuttgart.graphengine.dto.vFlow.VFlowEdgeDTO;
import de.unistuttgart.graphengine.dto.vFlow.VFlowGraphDTO;
import de.unistuttgart.graphengine.dto.vFlow.VFlowNodeDTO;
import de.unistuttgart.graphengine.dto.vFlow.VFlowNodeDataDTO;
import de.unistuttgart.graphengine.dto.vFlow.PointDTO;
import de.unistuttgart.graphengine.dto.transformationrule.GraphDTO;
import de.unistuttgart.graphengine.dto.transformationrule.InputDTO;
import de.unistuttgart.graphengine.dto.transformationrule.NodeDTO;
import de.unistuttgart.graphengine.nodes.*;
import de.unistuttgart.graphengine.service.GraphMapper;
import de.unistuttgart.graphengine.validation_error.NodeConfigurationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GraphMapperTest {

    private GraphMapper graphMapper;

    @BeforeEach
    void setUp() {
        graphMapper = new GraphMapper();
    }

    // ==========================================================================================
    // SECTION 1: Tests for vflowToGraphDto
    // ==========================================================================================
    @Nested
    @DisplayName("VFlow DTO to Graph DTO Mapping")
    class VFlowToGraphDtoMapping {

        @Test
        @DisplayName("should return empty GraphDTO for null VFlow input")
        void shouldReturnEmptyGraphDtoForNullVFlowInput() {
            GraphDTO result = graphMapper.vflowToGraphDto(null);
            assertNotNull(result);
            assertNull(result.getNodes());
        }

        @Test
        @DisplayName("should return empty GraphDTO for VFlow with no nodes")
        void shouldReturnEmptyGraphDtoForVFlowWithNoNodes() {
            VFlowGraphDTO vflowDto = new VFlowGraphDTO();
            vflowDto.setNodes(Collections.emptyList());
            GraphDTO result = graphMapper.vflowToGraphDto(vflowDto);
            assertNotNull(result);
            assertNull(result.getNodes());
        }

        @Test
        @DisplayName("should map VFlow nodes correctly when edges are null")
        void shouldMapNodesCorrectlyWhenEdgesAreNull() {
            VFlowGraphDTO vflowDto = new VFlowGraphDTO();
            vflowDto.setNodes(List.of(
                    createVFlowNode("1", "CONSTANT", "Constant A", 100.0)
            ));
            vflowDto.setEdges(null); // Explicitly null

            GraphDTO result = graphMapper.vflowToGraphDto(vflowDto);
            assertEquals(1, result.getNodes().size());
            NodeDTO nodeDto = result.getNodes().get(0);
            assertEquals(1, nodeDto.getId());
            assertEquals("Constant A", nodeDto.getName());
            assertTrue(nodeDto.getInputNodes().isEmpty());
        }

        @Test
        @DisplayName("should map a complete VFlow graph with nodes and edges")
        void shouldMapCompleteVFlowGraph() {
            // ARRANGE
            VFlowGraphDTO vflowDto = new VFlowGraphDTO();
            VFlowNodeDTO constantNode = createVFlowNode("10", "CONSTANT", "Is Active", true);
            VFlowNodeDTO providerNode = createVFlowNode("20", "PROVIDER", "Temperature", null);
            providerNode.getData().setJsonPath("$.temp");
            providerNode.getData().setArcId(123);
            VFlowNodeDTO logicNode = createVFlowNode("30", "LOGIC", "Check Temp", null);
            logicNode.getData().setOperatorType("GREATER_THAN");

            vflowDto.setNodes(List.of(constantNode, providerNode, logicNode));

            VFlowEdgeDTO edge1 = createVFlowEdge("10", "30", "input-1");
            VFlowEdgeDTO edge2 = createVFlowEdge("20", "30", "input-0");
            vflowDto.setEdges(List.of(edge1, edge2));

            // ACT
            GraphDTO result = graphMapper.vflowToGraphDto(vflowDto);

            // ASSERT
            assertEquals(3, result.getNodes().size());
            NodeDTO resultLogicNode = findNodeDtoById(result, 30);
            assertNotNull(resultLogicNode);
            assertEquals("LOGIC", resultLogicNode.getNodeType());
            assertEquals("GREATER_THAN", resultLogicNode.getOperatorType());

            assertEquals(2, resultLogicNode.getInputNodes().size());
            InputDTO inputFromProvider = findInputDtoById(resultLogicNode, 20);
            InputDTO inputFromConstant = findInputDtoById(resultLogicNode, 10);

            assertNotNull(inputFromProvider);
            assertEquals(0, inputFromProvider.getOrderIndex()); // from "input-0"
            assertNotNull(inputFromConstant);
            assertEquals(1, inputFromConstant.getOrderIndex()); // from "input-1"
        }

        @Test
        @DisplayName("should gracefully handle edges with non-existent targets")
        void shouldHandleEdgesWithInvalidTargets() {
            VFlowGraphDTO vflowDto = new VFlowGraphDTO();
            vflowDto.setNodes(List.of(createVFlowNode("1", "CONSTANT", "Node 1", null)));
            // This edge points to a target "99" that doesn't exist
            vflowDto.setEdges(List.of(createVFlowEdge("1", "99", "input-0")));

            GraphDTO result = graphMapper.vflowToGraphDto(vflowDto);
            assertEquals(1, result.getNodes().size());
            // The node exists, but no input should have been added as the edge was invalid
            assertTrue(result.getNodes().get(0).getInputNodes().isEmpty());
        }

        @Test
        @DisplayName("should parse various target handles for orderIndex")
        void shouldParseTargetHandles() {
            VFlowGraphDTO vflowDto = new VFlowGraphDTO();
            vflowDto.setNodes(List.of(
                    createVFlowNode("1", "SOURCE", "Source 1", null),
                    createVFlowNode("2", "TARGET", "Target", null)
            ));
            vflowDto.setEdges(List.of(
                    createVFlowEdge("1", "2", "input-5"),      // valid
                    createVFlowEdge("1", "2", "input-abc"),     // invalid format
                    createVFlowEdge("1", "2", null),            // null handle
                    createVFlowEdge("1", "2", "no-delimiter")   // no delimiter
            ));

            GraphDTO result = graphMapper.vflowToGraphDto(vflowDto);
            NodeDTO targetNode = findNodeDtoById(result, 2);

            assertEquals(4, targetNode.getInputNodes().size());
            assertTrue(targetNode.getInputNodes().stream().anyMatch(i -> i.getOrderIndex() == 5));
            assertTrue(targetNode.getInputNodes().stream().anyMatch(i -> i.getOrderIndex() == 0));
            // Check that 3 out of 4 edges defaulted to orderIndex 0
            assertEquals(3, targetNode.getInputNodes().stream().filter(i -> i.getOrderIndex() == 0).count());
        }
    }


    // ==========================================================================================
    // SECTION 2: Tests for toNodeGraph
    // ==========================================================================================
    @Nested
    @DisplayName("Graph DTO to Node Graph Mapping")
    class GraphDtoToNodeGraphMapping {

        @Test
        @DisplayName("should return empty result for null GraphDTO")
        void shouldReturnEmptyResultForNullGraphDto() {
            GraphMapper.MappingResult result = graphMapper.toNodeGraph(null);
            assertTrue(result.nodes().isEmpty());
            assertTrue(result.mappingErrors().isEmpty());
        }

        @Test
        @DisplayName("should return empty result for GraphDTO with no nodes")
        void shouldReturnEmptyResultForGraphDtoWithNoNodes() {
            GraphDTO graphDto = new GraphDTO();
            graphDto.setNodes(Collections.emptyList());
            GraphMapper.MappingResult result = graphMapper.toNodeGraph(graphDto);
            assertTrue(result.nodes().isEmpty());
            assertTrue(result.mappingErrors().isEmpty());
        }

        @Test
        @DisplayName("should map all node types correctly")
        void shouldMapAllNodeTypes() {
            // ARRANGE
            GraphDTO graphDto = new GraphDTO();
            NodeDTO constantDto = createNodeDTO(1, "CONSTANT", "My Const");
            constantDto.setValue(42);

            NodeDTO providerDto = createNodeDTO(2, "PROVIDER", "My Provider");
            // KORREKTUR: Der jsonPath muss mit "source." beginnen und mind. 2 Teile haben.
            providerDto.setJsonPath("source.sensor.temperature");

            NodeDTO logicDto = createNodeDTO(3, "LOGIC", "My Logic");
            logicDto.setOperatorType("ADD");

            NodeDTO finalDto = createNodeDTO(4, "FINAL", "Final");
            NodeDTO configDto = createNodeDTO(5, "CONFIG", "Config");

            graphDto.setNodes(List.of(constantDto, providerDto, logicDto, finalDto, configDto));

            // ACT
            GraphMapper.MappingResult result = graphMapper.toNodeGraph(graphDto);

            // ASSERT
            assertEquals(5, result.nodes().size());
            assertTrue(result.mappingErrors().isEmpty());
            assertTrue(findNodeById(result.nodes(), 1) instanceof ConstantNode);
            assertTrue(findNodeById(result.nodes(), 2) instanceof ProviderNode);
            assertTrue(findNodeById(result.nodes(), 3) instanceof LogicNode);
            assertTrue(findNodeById(result.nodes(), 4) instanceof FinalNode);
            assertTrue(findNodeById(result.nodes(), 5) instanceof ConfigNode);
        }

        @Test
        @DisplayName("should map ConfigNode with all its properties")
        void shouldMapConfigNodeProperties() {
            GraphDTO graphDto = new GraphDTO();
            NodeDTO configDto = createNodeDTO(1, "CONFIG", "Config");
            configDto.setChangeDetectionActive(true);
            configDto.setChangeDetectionMode("OR");
            configDto.setTimeWindowEnabled(true);
            configDto.setTimeWindowMillis(5000L);
            graphDto.setNodes(List.of(configDto));

            GraphMapper.MappingResult result = graphMapper.toNodeGraph(graphDto);

            assertEquals(1, result.nodes().size());
            ConfigNode configNode = (ConfigNode) result.nodes().get(0);
            assertTrue(configNode.isActive());
            assertEquals(ConfigNode.ChangeDetectionMode.OR, configNode.getMode());
            assertTrue(configNode.isTimeWindowEnabled());
            assertEquals(5000L, configNode.getTimeWindowMillis());
        }

        @Test
        @DisplayName("should connect nodes and respect input orderIndex")
        void shouldConnectNodesAndRespectOrder() {
            GraphDTO graphDto = new GraphDTO();

            NodeDTO source1 = createNodeDTO(10, "CONSTANT", "Source 1");
            source1.setValue(100);

            NodeDTO source2 = createNodeDTO(20, "CONSTANT", "Source 2");
            source2.setValue(200);

            NodeDTO target = createNodeDTO(30, "LOGIC", "Target");
            target.setOperatorType("ADD");

            // KORREKTUR: Erzeuge eine veränderliche ArrayList statt einer unveränderlichen List.of()
            target.setInputNodes(new ArrayList<>(List.of(
                    createInputDTO(20, 1), // source2 should be at index 1
                    createInputDTO(10, 0)  // source1 should be at index 0
            )));
            graphDto.setNodes(List.of(source1, source2, target));

            GraphMapper.MappingResult result = graphMapper.toNodeGraph(graphDto);

            assertEquals(3, result.nodes().size());
            Node targetNode = findNodeById(result.nodes(), 30);
            assertNotNull(targetNode.getInputNodes());
            assertEquals(2, targetNode.getInputNodes().size());

            // Assert correct order
            assertEquals(10, targetNode.getInputNodes().get(0).getId());
            assertEquals(20, targetNode.getInputNodes().get(1).getId());
        }

        @Test
        @DisplayName("should create mapping error for unknown nodeType and skip node")
        void shouldCreateErrorForUnknownNodeType() {
            GraphDTO graphDto = new GraphDTO();
            NodeDTO validDto = createNodeDTO(1, "FINAL", "Valid Node");
            NodeDTO invalidDto = createNodeDTO(2, "UNKNOWN_TYPE", "Invalid Node");
            graphDto.setNodes(List.of(validDto, invalidDto));

            GraphMapper.MappingResult result = graphMapper.toNodeGraph(graphDto);

            // One valid node should be created
            assertEquals(1, result.nodes().size());
            assertEquals(1, findNodeById(result.nodes(), 1).getId());
            // One error should be recorded
            assertEquals(1, result.mappingErrors().size());
            assertTrue(result.mappingErrors().get(0) instanceof NodeConfigurationError);
            assertEquals(2, ((NodeConfigurationError) result.mappingErrors().get(0)).getNodeId());
        }

        @Test
        @DisplayName("should fail on invalid operatorType with IllegalArgumentException")
        void shouldFailOnInvalidOperatorType() {
            GraphDTO graphDto = new GraphDTO();
            NodeDTO logicDto = createNodeDTO(1, "LOGIC", "Invalid Logic");
            logicDto.setOperatorType("NON_EXISTENT_OPERATOR");
            graphDto.setNodes(List.of(logicDto));

            // The current implementation does not catch this, so the whole method should fail.
            assertThrows(IllegalArgumentException.class, () -> {
                graphMapper.toNodeGraph(graphDto);
            });
        }
    }


    // ==========================================================================================
    // SECTION 3: Helper Methods for creating DTOs
    // ==========================================================================================

    private VFlowNodeDTO createVFlowNode(String id, String nodeType, String name, Object value) {
        VFlowNodeDTO node = new VFlowNodeDTO();
        node.setId(id);

        PointDTO point = new PointDTO();
        point.setX(100);
        point.setY(100);
        node.setPoint(point);

        VFlowNodeDataDTO data = new VFlowNodeDataDTO();
        data.setNodeType(nodeType);
        data.setName(name);
        data.setValue(value);
        node.setData(data);

        return node;
    }

    private VFlowEdgeDTO createVFlowEdge(String sourceId, String targetId, String targetHandle) {
        VFlowEdgeDTO edge = new VFlowEdgeDTO();
        edge.setSource(sourceId);
        edge.setTarget(targetId);
        edge.setTargetHandle(targetHandle);
        return edge;
    }

    private NodeDTO createNodeDTO(int id, String nodeType, String name) {
        NodeDTO dto = new NodeDTO();
        dto.setId(id);
        dto.setNodeType(nodeType);
        dto.setName(name);
        dto.setInputNodes(Collections.emptyList());
        return dto;
    }

    private InputDTO createInputDTO(int sourceId, int orderIndex) {
        InputDTO dto = new InputDTO();
        dto.setId(sourceId);
        dto.setOrderIndex(orderIndex);
        return dto;
    }

    private NodeDTO findNodeDtoById(GraphDTO graphDto, int id) {
        return graphDto.getNodes().stream().filter(n -> n.getId() == id).findFirst().orElse(null);
    }

    private InputDTO findInputDtoById(NodeDTO nodeDto, int id) {
        return nodeDto.getInputNodes().stream().filter(i -> i.getId() == id).findFirst().orElse(null);
    }

    private Node findNodeById(List<Node> nodes, int id) {
        return nodes.stream().filter(n -> n.getId() == id).findFirst().orElse(null);
    }
}