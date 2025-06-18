package de.unistuttgart.stayinsync.syncnode.logik_engine.Database;

import de.unistuttgart.stayinsync.syncnode.logik_engine.Database.DTOs.GraphDefinitionDTO;
import de.unistuttgart.stayinsync.syncnode.logik_engine.Database.DTOs.InputDTO;
import de.unistuttgart.stayinsync.syncnode.logik_engine.Database.DTOs.NodeDTO;
import de.unistuttgart.stayinsync.syncnode.logik_engine.*;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the conversion between the logic engine's domain objects (e.g., {@link LogicNode})
 * and the Data Transfer Objects (DTOs) used for persistence.
 */
@ApplicationScoped
public class GraphMapper {

    // =================================================================================
    // MAPPING: Logic Node  ==>  TO  ==>  DTOs (for saving to DB)
    // =================================================================================

    public GraphDefinitionDTO graphToDto(List<LogicNode> nodes) {
        GraphDefinitionDTO graphDto = new GraphDefinitionDTO();
        List<NodeDTO> nodeDtos = new ArrayList<>();

        for (LogicNode node : nodes) {
            NodeDTO nodeDto = logicNodeToDto(node);
            nodeDtos.add(nodeDto);
        }

        graphDto.nodes = nodeDtos;
        return graphDto;
    }

    private NodeDTO logicNodeToDto(LogicNode node) {
        NodeDTO dto = new NodeDTO();
        dto.nodeName = node.getNodeName();
        dto.operator = node.getOperator().name();

        List<InputDTO> inputDtos = new ArrayList<>();
        List<InputNode> inputProviders = node.getInputProviders();

        for (InputNode input : inputProviders) {
            InputDTO inputDto = inputNodeToDto(input);
            inputDtos.add(inputDto);
        }
        dto.inputs = inputDtos;
        return dto;
    }

    private InputDTO inputNodeToDto(InputNode input) {
        InputDTO dto = new InputDTO();
        if (input.isConstantNode()) {
            dto.type = "CONSTANT";
            dto.elementName = ((ConstantNode) input).getElementName();
            dto.value = ((ConstantNode) input).getValue();
        } else if (input.isJsonNode()) {
            JsonNode jsonNode = (JsonNode) input;
            dto.type = "JSON";
            dto.sourceName = jsonNode.getSourceName();
            dto.path = jsonNode.getJsonPath();
        } else if (input.isParentNode()) {
            dto.type = "PARENT";
            dto.parentNodeName = input.getParentNode().getNodeName();
        } else {
            throw new IllegalArgumentException("Unknown InputNode type for serialization: " + input.getClass().getName());
        }
        return dto;
    }

    // =================================================================================
    // MAPPING: DTOs (from DB)  ==>  TO  ==>  LogicNode
    // =================================================================================

    public List<LogicNode> toLogicNode(GraphDefinitionDTO graphDto) {
        Map<String, LogicNode> createdNodes = new HashMap<>();
        List<LogicNode> allNodesInList = new ArrayList<>();

        // Phase 1: Create all node instances (placeholders)
        for (NodeDTO nodeDto : graphDto.nodes) {
            LogicOperator operator = LogicOperator.valueOf(nodeDto.operator);
            LogicNode node = new LogicNode(nodeDto.nodeName, operator); // Uses the "mapper" constructor
            createdNodes.put(node.getNodeName(), node);
            allNodesInList.add(node);
        }

        // Phase 2: Create inputs and establish connections
        for (NodeDTO nodeDto : graphDto.nodes) {
            LogicNode currentNode = createdNodes.get(nodeDto.nodeName);
            List<InputNode> resolvedInputs = new ArrayList<>();

            for (InputDTO inputDto : nodeDto.inputs) {
                InputNode inputNode = toInputNode(inputDto, createdNodes);
                resolvedInputs.add(inputNode);
            }

            currentNode.setInputProviders(resolvedInputs); // Uses the setter
        }

        return allNodesInList;
    }

    private InputNode toInputNode(InputDTO dto, Map<String, LogicNode> createdNodes) {
        switch (dto.type) {
            case "CONSTANT":
                return new ConstantNode(dto.elementName, dto.value);
            case "JSON":
                return new JsonNode(dto.sourceName, dto.path);
            case "PARENT":
                LogicNode parentNode = createdNodes.get(dto.parentNodeName);
                if (parentNode == null) {
                    throw new IllegalStateException("Graph inconsistency: Parent node '" + dto.parentNodeName + "' not found during deserialization.");
                }
                return new ParentNode(parentNode);
            default:
                throw new IllegalArgumentException("Unknown InputDTO type during deserialization: " + dto.type);
        }
    }
}
