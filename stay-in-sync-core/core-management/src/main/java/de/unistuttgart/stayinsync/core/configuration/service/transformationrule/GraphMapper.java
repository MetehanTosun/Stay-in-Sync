package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;

import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.InputDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.NodeDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow.*;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.LogicOperator;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.*;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;

/**
 * A central service for mapping between different graph representations:
 * - VFlowGraphDTO (from the frontend)
 * - List<Node> (the internal, in-memory domain model)
 * - GraphDTO (the flattened format for database persistence)
 */
@ApplicationScoped
public class GraphMapper {

    // ==========================================================================================
    // SECTION 1: Mapping from Frontend (VFlow) to Persistence Format (GraphDTO)
    // ==========================================================================================

    /**
     * Converts a VFlowGraphDTO from the frontend into the flattened GraphDTO format
     * that is used for persistence in the database.
     *
     * @param vflowDto The graph data from the ngx-vflow UI.
     * @return The flattened GraphDTO ready for persistence.
     */
    public GraphDTO vflowToGraphDto(VFlowGraphDTO vflowDto) {
        if (vflowDto == null || vflowDto.getNodes() == null) {
            return new GraphDTO();
        }

        GraphDTO graphDto = new GraphDTO();

        // Pass 1: Create all flat NodeDTOs from the vflow nodes.
        Map<String, NodeDTO> nodeDtoMap = mapVFlowNodesToNodeDTOs(vflowDto.getNodes());

        // Pass 2: Apply the edge information to create the `inputNodes` connections.
        if (vflowDto.getEdges() != null) {
            applyVFlowEdgesToInputNodes(nodeDtoMap, vflowDto.getEdges());
        }

        graphDto.setNodes(new ArrayList<>(nodeDtoMap.values()));
        return graphDto;
    }

    /**
     * Helper to create a map of flattened NodeDTOs from the VFlowNodeDTO list.
     */
    private Map<String, NodeDTO> mapVFlowNodesToNodeDTOs(List<VFlowNodeDTO> vflowNodes) {
        Map<String, NodeDTO> nodeDtoMap = new HashMap<>();
        for (VFlowNodeDTO vflowNode : vflowNodes) {
            NodeDTO nodeDto = new NodeDTO();
            VFlowNodeDataDTO data = vflowNode.getData();

            nodeDto.setId(Integer.parseInt(vflowNode.getId()));
            nodeDto.setName(data.getName());
            nodeDto.setNodeType(data.getNodeType());
            nodeDto.setOffsetX(vflowNode.getPoint().getX());
            nodeDto.setOffsetY(vflowNode.getPoint().getY());
            nodeDto.setArcId(data.getArcId());
            nodeDto.setJsonPath(data.getJsonPath());
            nodeDto.setValue(data.getValue());
            nodeDto.setOperatorType(data.getOperatorType());
            nodeDto.setInputTypes(data.getInputTypes());
            nodeDto.setOutputType(data.getOutputType());
            nodeDto.setInputLimit(data.getInputLimit());
            nodeDto.setInputNodes(new ArrayList<>()); // Initialize empty list for connections

            nodeDtoMap.put(vflowNode.getId(), nodeDto);
        }
        return nodeDtoMap;
    }

    /**
     * Helper that iterates through the edges and adds the corresponding
     * InputDTOs to the target NodeDTOs' `inputNodes` list.
     */
    private void applyVFlowEdgesToInputNodes(Map<String, NodeDTO> nodeDtoMap, List<VFlowEdgeDTO> vflowEdges) {
        for (VFlowEdgeDTO edge : vflowEdges) {
            NodeDTO targetNodeDto = nodeDtoMap.get(edge.getTarget());
            if (targetNodeDto == null) {
                Log.warnf("Edge references a non-existent target node with ID: %s", edge.getTarget());
                continue;
            }

            InputDTO inputDto = new InputDTO();
            inputDto.setId(Integer.parseInt(edge.getSource()));
            inputDto.setOrderIndex(parseOrderIndexFromTargetHandle(edge.getTargetHandle()));

            targetNodeDto.getInputNodes().add(inputDto);
        }
    }

    // ==========================================================================================
    // SECTION 2: Mapping from Persistence Format (GraphDTO) back to Frontend (VFlowGraphDTO)
    // ==========================================================================================

    /**
     * Converts a flattened GraphDTO (from the database) back into the VFlowGraphDTO format
     * that the frontend UI expects.
     *
     * @param graphDto The flattened graph data from the database.
     * @return The VFlowGraphDTO with separate lists for nodes and edges.
     */
    public VFlowGraphDTO graphToVFlowDto(GraphDTO graphDto, String ruleName, String description) {
        if (graphDto == null) {
            return new VFlowGraphDTO();
        }

        VFlowGraphDTO vflowDto = new VFlowGraphDTO();
        vflowDto.setNodes(mapNodeDTOsToVFlowNodes(graphDto.getNodes()));
        // Reconstruct the edge list for the frontend from the inputNodes property.
        vflowDto.setEdges(createVFlowEdgesFromNodeDTOs(graphDto.getNodes()));

        return vflowDto;
    }

    /**
     * Helper to map a list of persistence NodeDTOs to a list of VFlowNodeDTOs.
     */
    private List<VFlowNodeDTO> mapNodeDTOsToVFlowNodes(List<NodeDTO> nodeDtos) {
        // ... (this method remains the same as before) ...
        if(nodeDtos == null) return new ArrayList<>();
        List<VFlowNodeDTO> vflowNodes = new ArrayList<>();
        for (NodeDTO nodeDto : nodeDtos) {
            VFlowNodeDTO vflowNode = new VFlowNodeDTO();
            vflowNode.setId(String.valueOf(nodeDto.getId()));

            PointDTO point = new PointDTO();
            point.setX(nodeDto.getOffsetX());
            point.setY(nodeDto.getOffsetY());
            vflowNode.setPoint(point);
            vflowNode.setType(nodeDto.getNodeType());

            VFlowNodeDataDTO data = new VFlowNodeDataDTO();
            data.setName(nodeDto.getName());
            data.setNodeType(nodeDto.getNodeType());
            data.setArcId(nodeDto.getArcId());
            data.setJsonPath(nodeDto.getJsonPath());
            data.setValue(nodeDto.getValue());
            data.setOperatorType(nodeDto.getOperatorType());
            data.setInputTypes(nodeDto.getInputTypes());
            data.setOutputType(nodeDto.getOutputType());
            data.setInputLimit(nodeDto.getInputLimit());

            vflowNode.setData(data);
            vflowNodes.add(vflowNode);
        }
        return vflowNodes;
    }

    /**
     * Helper to reconstruct the list of VFlowEdgeDTOs from the flattened inputNodes properties.
     */
    private List<VFlowEdgeDTO> createVFlowEdgesFromNodeDTOs(List<NodeDTO> nodeDtos) {
        if(nodeDtos == null) return new ArrayList<>();
        List<VFlowEdgeDTO> vflowEdges = new ArrayList<>();
        for (NodeDTO targetNodeDto : nodeDtos) {
            if (targetNodeDto.getInputNodes() != null) {
                for (InputDTO inputDto : targetNodeDto.getInputNodes()) {
                    VFlowEdgeDTO edge = new VFlowEdgeDTO();
                    String sourceIdStr = String.valueOf(inputDto.getId());
                    String targetIdStr = String.valueOf(targetNodeDto.getId());

                    edge.setSource(sourceIdStr);
                    edge.setTarget(targetIdStr);
                    edge.setId(sourceIdStr + " -> " + targetIdStr);
                    edge.setTargetHandle("input-" + inputDto.getOrderIndex());

                    vflowEdges.add(edge);
                }
            }
        }
        return vflowEdges;
    }

    // ==========================================================================================
    // SECTION 3: Mapping from Persistence Format (GraphDTO) to Internal Domain Model (List<Node>)
    // ==========================================================================================

    /**
     * Maps a GraphDTO (deserialized from JSON) into a fully connected
     * in-memory graph of Node objects for validation and evaluation.
     */
    public List<Node> toNodeGraph(GraphDTO graphDto) {
        if (graphDto == null || graphDto.getNodes() == null) {
            return new ArrayList<>();
        }
        Map<Integer, Node> createdNodes = new HashMap<>();

        // Pass 1: Create all node instances.
        for (NodeDTO dto : graphDto.getNodes()) {
            Node node = null;
            switch (dto.getNodeType()) {
                case "PROVIDER":
                    node = new ProviderNode(dto.getJsonPath());
                    ((ProviderNode) node).setArcId(dto.getArcId());
                    break;
                case "CONSTANT":
                    node = new ConstantNode(dto.getName(), dto.getValue());
                    break;
                case "LOGIC":
                    node = new LogicNode(dto.getName(), LogicOperator.valueOf(dto.getOperatorType()));
                    break;
                case "FINAL":
                    node = new FinalNode();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown nodeType: " + dto.getNodeType());
            }
            node.setId(dto.getId());
            node.setName(dto.getName());
            node.setOffsetX(dto.getOffsetX());
            node.setOffsetY(dto.getOffsetY());
            createdNodes.put(node.getId(), node);
        }

        // Pass 2: Apply input connections from the inputNodes property of each NodeDTO.
        for (NodeDTO dto : graphDto.getNodes()) {
            Node targetNode = createdNodes.get(dto.getId());
            if (dto.getInputNodes() != null && !dto.getInputNodes().isEmpty()) {
                // Ensure the input list is sorted by orderIndex
                dto.getInputNodes().sort(Comparator.comparingInt(InputDTO::getOrderIndex));

                List<Node> orderedInputs = new ArrayList<>();
                for (InputDTO inputDto : dto.getInputNodes()) {
                    Node sourceNode = createdNodes.get(inputDto.getId());
                    if (sourceNode != null) {
                        orderedInputs.add(sourceNode);
                    }
                }
                targetNode.setInputNodes(orderedInputs);
            }
        }

        return new ArrayList<>(createdNodes.values());
    }

    /**
     * Extracts the numeric order index from a vflow target handle string.
     */
    private int parseOrderIndexFromTargetHandle(String targetHandle) {
        if (targetHandle != null && targetHandle.contains("-")) {
            try {
                return Integer.parseInt(targetHandle.split("-")[1]);
            } catch (Exception e) {
                Log.warnf("Could not parse orderIndex from targetHandle: %s. Defaulting to 0.", targetHandle);
            }
        }
        return 0;
    }
}