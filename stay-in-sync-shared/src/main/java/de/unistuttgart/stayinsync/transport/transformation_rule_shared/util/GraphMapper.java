package de.unistuttgart.stayinsync.transport.transformation_rule_shared.util;

import de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow.VFlowEdgeDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow.VFlowGraphDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow.VFlowNodeDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow.VFlowNodeDataDTO;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.logic_operator.LogicOperator;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.ConstantNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.LogicNode;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.Node;
import de.unistuttgart.stayinsync.transport.transformation_rule_shared.nodes.ProviderNode;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.InputDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.NodeDTO;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.*;

@ApplicationScoped
public class GraphMapper {


    /**
     * Converts a VFlowGraphDTO from the frontend into the flattened GraphDTO format
     * that is used for persistence in the database.
     *
     * @param vflowDto The graph data from the ngx-vflow UI, containing separate node and edge lists.
     * @return The flattened GraphDTO ready for persistence.
     */
    public GraphDTO vflowToGraphDto(VFlowGraphDTO vflowDto) {
        if (vflowDto == null || vflowDto.getNodes() == null) {
            return new GraphDTO();
        }

        GraphDTO graphDto = new GraphDTO();
        graphDto.setName(vflowDto.getName());

        // Pass 1: Create all flat NodeDTOs from the vflow nodes.
        Map<String, NodeDTO> nodeDtoMap = mapVFlowNodesToNodeDTOs(vflowDto.getNodes());

        // Pass 2: Apply the edge information to create the input connections.
        if (vflowDto.getEdges() != null) {
            applyVFlowEdgesToNodeDTOs(nodeDtoMap, vflowDto.getEdges());
        }

        graphDto.setNodes(new ArrayList<>(nodeDtoMap.values()));
        return graphDto;
    }

    /**
     * Helper for Pass 1: Creates a map of flattened NodeDTOs from the VFlowNodeDTO list.
     *
     * @param vflowNodes The list of nodes received from the frontend.
     * @return A map of NodeDTOs, keyed by their string ID.
     */
    private Map<String, NodeDTO> mapVFlowNodesToNodeDTOs(List<VFlowNodeDTO> vflowNodes) {
        Map<String, NodeDTO> nodeDtoMap = new HashMap<>();
        for (VFlowNodeDTO vflowNode : vflowNodes) {
            NodeDTO nodeDto = new NodeDTO();
            VFlowNodeDataDTO data = vflowNode.getData();

            // Copy all properties from the VFlow DTO to the persistence DTO
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
            nodeDto.setInputNodes(new ArrayList<>());

            nodeDtoMap.put(vflowNode.getId(), nodeDto);
        }
        return nodeDtoMap;
    }

    /**
     * Helper for Pass 2: Iterates through the edges and adds the corresponding
     * InputDTOs to the target NodeDTOs.
     *
     * @param nodeDtoMap A map of all created NodeDTOs, keyed by their string ID.
     * @param vflowEdges The list of edges received from the frontend.
     */
    private void applyVFlowEdgesToNodeDTOs(Map<String, NodeDTO> nodeDtoMap, List<VFlowEdgeDTO> vflowEdges) {
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

    /**
     * Extracts the numeric order index from a vflow target handle string.
     * Example: "input-0" -> 0
     */
    private int parseOrderIndexFromTargetHandle(String targetHandle) {
        if (targetHandle != null && targetHandle.contains("-")) {
            try {
                String[] parts = targetHandle.split("-");
                return Integer.parseInt(parts[1]);
            } catch (Exception e) {
                Log.warnf("Could not parse orderIndex from targetHandle: %s. Defaulting to 0.", targetHandle);
            }
        }
        return 0; // Default to 0 if handle is missing or malformed
    }

    /**
     * Maps a list of in-memory Node objects to a serializable GraphDTO.
     */
    public GraphDTO graphToDto(List<Node> nodes) {
        GraphDTO graphDto = new GraphDTO();

        List<NodeDTO> nodeDtos = new ArrayList<>();
        for (Node node : nodes) {
            nodeDtos.add(this.nodeToDto(node));
        }

        graphDto.setNodes(nodeDtos);
        return graphDto;
    }

    /**
     * Helper method to map a single Node object to its DTO representation.
     */
    private NodeDTO nodeToDto(Node node) {
        NodeDTO dto = new NodeDTO();
        dto.setId(node.getId());
        dto.setName(node.getName());
        dto.setOffsetX(node.getOffsetX());
        dto.setOffsetY(node.getOffsetY());

        // Create InputDTOs from the list of parent nodes
        if (node.getInputNodes() != null) {
            List<InputDTO> inputDtos = new ArrayList<>();
            for (int i = 0; i < node.getInputNodes().size(); i++) {
                Node parentNode = node.getInputNodes().get(i);
                InputDTO inputDto = new InputDTO();
                inputDto.setId(parentNode.getId());
                inputDto.setOrderIndex(i);
                inputDtos.add(inputDto);
            }
            dto.setInputNodes(inputDtos);
        }

        // Set type-specific properties
        if (node instanceof ProviderNode) {
            ProviderNode provider = (ProviderNode) node;
            dto.setNodeType("PROVIDER");
            dto.setArcId(provider.getArcId());
            dto.setJsonPath(provider.getJsonPath());
        } else if (node instanceof ConstantNode) {
            ConstantNode constant = (ConstantNode) node;
            dto.setNodeType("CONSTANT");
            dto.setValue(constant.getValue());
        } else if (node instanceof LogicNode) {
            LogicNode logic = (LogicNode) node;
            dto.setNodeType("LOGIC");
            dto.setOperatorType(logic.getOperator().name());
        }
        return dto;
    }

    /**
     * Maps a GraphDTO (deserialized from JSON) back to a fully connected
     * in-memory graph represented by a list of Node objects.
     * This is a two-pass process.
     */
    public List<Node> toNodeGraph(GraphDTO graphDto) {
        Map<Integer, Node> createdNodes = new HashMap<>();
        List<Node> allNodesInList = new ArrayList<>();

        // --- Pass 1: Create all node instances without their connections ---
        for (NodeDTO dto : graphDto.getNodes()) {
            Node node = null;
            switch (dto.getNodeType()) {
                case "PROVIDER":
                    ProviderNode provider = new ProviderNode(dto.getJsonPath());
                    provider.setArcId(dto.getArcId());
                    node = provider;
                    break;
                case "CONSTANT":
                    node = new ConstantNode(dto.getName(), dto.getValue());
                    break;
                case "LOGIC":
                    LogicOperator operator = LogicOperator.valueOf(dto.getOperatorType());
                    node = new LogicNode(dto.getName(), operator);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown nodeType during deserialization: " + dto.getNodeType());
            }

            // Set common properties
            node.setId(dto.getId());
            if (!"CONSTANT".equals(dto.getNodeType()))
                node.setName(dto.getName()); // Name for ConstantNode is set via constructor
            node.setOffsetX(dto.getOffsetX());
            node.setOffsetY(dto.getOffsetY());

            createdNodes.put(node.getId(), node);
            allNodesInList.add(node);
        }

        // --- Pass 2: Establish the connections (edges) between the nodes ---
        for (NodeDTO dto : graphDto.getNodes()) {
            Node currentNode = createdNodes.get(dto.getId());
            List<InputDTO> inputDtos = dto.getInputNodes();

            if (inputDtos != null && !inputDtos.isEmpty()) {
                // Prepare a correctly ordered list for the inputs
                Node[] orderedInputs = new Node[inputDtos.size()];
                for (InputDTO inputDto : inputDtos) {
                    Node parentNode = createdNodes.get(inputDto.getId());
                    if (parentNode == null) {
                        throw new IllegalStateException("Graph inconsistent: Parent node with ID " + inputDto.getId() + " not found.");
                    }
                    orderedInputs[inputDto.getOrderIndex()] = parentNode;
                }
                currentNode.setInputNodes(Arrays.asList(orderedInputs));
            }
        }

        return allNodesInList;
    }
}