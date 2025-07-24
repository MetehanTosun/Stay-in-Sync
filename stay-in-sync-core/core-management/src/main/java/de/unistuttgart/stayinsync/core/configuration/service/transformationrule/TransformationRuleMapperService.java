package de.unistuttgart.stayinsync.core.configuration.service.transformationrule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.stayinsync.core.configuration.domain.entities.sync.TransformationRule;
import de.unistuttgart.stayinsync.core.configuration.rest.dtos.TransformationRuleDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.GraphDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.InputDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.NodeDTO;
import de.unistuttgart.stayinsync.transport.dto.transformationrule.vFlow.*;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * A specialized mapper in the 'core-management' module.
 * Its purpose is to map the TransformationRule entity into various DTOs
 * required by the API layer for responses.
 */
@ApplicationScoped
public class TransformationRuleMapperService {

    @Inject
    ObjectMapper jsonObjectMapper;

    /**
     * Maps a TransformationRule entity to a lightweight DTO for list views.
     */
    public TransformationRuleDTO toRuleDTO(TransformationRule entity) {
        if (entity == null) return null;

        TransformationRuleDTO dto = new TransformationRuleDTO();
        dto.setId(entity.id);
        dto.setName(entity.name);
        dto.setDescription(entity.description);
        dto.setGraphStatus(entity.graphStatus);
        if (entity.transformation != null) {
            dto.setTransformationId(entity.transformation.id);
        }
        return dto;
    }

    // ==========================================================================================
    // HIER IST DIE FEHLENDE METHODE
    // ==========================================================================================
    /**
     * Maps a persisted TransformationRule entity to a full GraphDTO.
     * This is used by the API layer to build a detailed response for POST/PUT requests.
     *
     * @param entity The TransformationRule entity from the database.
     * @return The corresponding GraphDTO, including id, name, status, and node structure.
     */
    public GraphDTO toGraphDTO(TransformationRule entity) {
        if (entity == null || entity.graph == null || entity.graph.graphDefinitionJson == null) {
            return null;
        }

        try {
            // 1. Deserialize the JSON string into the basic GraphDTO structure (containing nodes).
            GraphDTO dto = jsonObjectMapper.readValue(entity.graph.graphDefinitionJson, GraphDTO.class);
            return dto;

        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to parse graph JSON for TransformationRule with id %d", entity.id);
            throw new RuntimeException("Failed to create GraphDTO from entity JSON.", e);
        }
    }

    /**
     * Converts the graph from a TransformationRule entity into the VFlowGraphDTO format
     * that the frontend UI expects.
     *
     * @param entity The TransformationRule entity from the database.
     * @return The VFlowGraphDTO with separate lists for nodes and edges.
     */
    public VFlowGraphDTO toVFlowDto(TransformationRule entity) {
        if (entity == null || entity.graph == null || entity.graph.graphDefinitionJson == null) {
            return new VFlowGraphDTO();
        }

        try {
            GraphDTO persistenceDto = jsonObjectMapper.readValue(entity.graph.graphDefinitionJson, GraphDTO.class);

            VFlowGraphDTO vflowDto = new VFlowGraphDTO();
            vflowDto.setName(entity.name);
            vflowDto.setDescription(entity.description);

            vflowDto.setNodes(mapNodeDTOsToVFlowNodes(persistenceDto.getNodes()));
            vflowDto.setEdges(createVFlowEdgesFromNodeDTOs(persistenceDto.getNodes()));

            return vflowDto;

        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to parse graph JSON for TransformationRule with id %d", entity.id);
            throw new RuntimeException("Failed to create VFlowGraphDTO from entity JSON.", e);
        }
    }

    /**
     * Helper to map a list of flattened NodeDTOs to a list of VFlowNodeDTOs.
     */
    private List<VFlowNodeDTO> mapNodeDTOsToVFlowNodes(List<NodeDTO> nodeDtos) {
        if (nodeDtos == null) return new ArrayList<>();
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
        if (nodeDtos == null) return new ArrayList<>();

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
}